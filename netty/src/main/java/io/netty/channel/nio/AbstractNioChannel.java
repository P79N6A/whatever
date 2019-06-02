package io.netty.channel.nio;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;
import io.netty.util.internal.ThrowableUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class AbstractNioChannel extends AbstractChannel {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractNioChannel.class);

    private static final ClosedChannelException DO_CLOSE_CLOSED_CHANNEL_EXCEPTION = ThrowableUtil.unknownStackTrace(new ClosedChannelException(), AbstractNioChannel.class, "doClose()");

    protected final int readInterestOp;
    /**
     * NIO Channel
     */
    private final SelectableChannel ch;
    /**
     * NIO SelectionKey
     */
    volatile SelectionKey selectionKey;
    /**
     * 底层读事件标记
     */
    boolean readPending;
    private final Runnable clearReadPendingRunnable = new Runnable() {
        @Override
        public void run() {
            clearReadPending0();
        }
    };

    /**
     * 异步连接结果
     */
    private ChannelPromise connectPromise;

    private ScheduledFuture<?> connectTimeoutFuture;
    private SocketAddress requestedRemoteAddress;

    /**
     * 构造方法
     */
    protected AbstractNioChannel(Channel parent, SelectableChannel ch, int readInterestOp) {
        super(parent);
        this.ch = ch;
        this.readInterestOp = readInterestOp;
        try {
            // 设置非阻塞
            ch.configureBlocking(false);
        } catch (IOException e) {
            try {
                ch.close();
            } catch (IOException e2) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Failed to close a partially initialized socket.", e2);
                }
            }

            throw new ChannelException("Failed to enter non-blocking mode.", e);
        }
    }
    //////////////////////////////////////////////////////////////////////////////////////////////////
    // whatever
    //////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean isOpen() {
        return ch.isOpen();
    }

    @Override
    public NioUnsafe unsafe() {
        return (NioUnsafe) super.unsafe();
    }

    protected SelectableChannel javaChannel() {
        return ch;
    }

    @Override
    public NioEventLoop eventLoop() {
        return (NioEventLoop) super.eventLoop();
    }

    protected SelectionKey selectionKey() {
        assert selectionKey != null;
        return selectionKey;
    }

    @Deprecated
    protected boolean isReadPending() {
        return readPending;
    }

    @Deprecated
    protected void setReadPending(final boolean readPending) {
        if (isRegistered()) {
            EventLoop eventLoop = eventLoop();
            if (eventLoop.inEventLoop()) {
                setReadPending0(readPending);
            } else {
                eventLoop.execute(new Runnable() {
                    @Override
                    public void run() {
                        setReadPending0(readPending);
                    }
                });
            }
        } else {

            this.readPending = readPending;
        }
    }

    protected final void clearReadPending() {
        if (isRegistered()) {
            EventLoop eventLoop = eventLoop();
            if (eventLoop.inEventLoop()) {
                clearReadPending0();
            } else {
                eventLoop.execute(clearReadPendingRunnable);
            }
        } else {
            readPending = false;
        }
    }

    private void setReadPending0(boolean readPending) {
        this.readPending = readPending;
        if (!readPending) {
            ((AbstractNioUnsafe) unsafe()).removeReadOp();
        }
    }

    private void clearReadPending0() {
        readPending = false;
        ((AbstractNioUnsafe) unsafe()).removeReadOp();
    }

    @Override
    protected boolean isCompatible(EventLoop loop) {
        return loop instanceof NioEventLoop;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////
    // do
    //////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void doRegister() throws Exception {
        boolean selected = false;
        for (; ; ) {
            try {
                // 将Channel注册到EventLoop的Selector上，0表示注册时不关心任何事件，attachment为Netty的Channel对象本身
                selectionKey = javaChannel().register(eventLoop().unwrappedSelector(), 0, this);
                return;
            } catch (CancelledKeyException e) {
                if (!selected) {
                    // 选择键取消重新selectNow()，清除因取消操作而缓存的选择键
                    eventLoop().selectNow();
                    selected = true;
                } else {
                    throw e;
                }
            }
        }
    }

    @Override
    protected void doDeregister() throws Exception {
        // 设置取消选择键
        // cancel操作调用后，注册关系不会立即生效，而会将cancel的key移入selector的一个取消键集合，
        // 当下次调用select相关方法或一个正在进行的select调用结束时，会从取消键集合中移除该选择键，此时注销才真正完成
        eventLoop().cancel(selectionKey());
    }

    @Override
    protected void doBeginRead() throws Exception {

        final SelectionKey selectionKey = this.selectionKey;
        if (!selectionKey.isValid()) {
            // 选择键被取消
            return;
        }
        // 设置底层读事件正在进行
        readPending = true;

        final int interestOps = selectionKey.interestOps();
        if ((interestOps & readInterestOp) == 0) {
            // 关心Read事件
            selectionKey.interestOps(interestOps | readInterestOp);
        }
    }

    protected abstract boolean doConnect(SocketAddress remoteAddress, SocketAddress localAddress) throws Exception;

    protected abstract void doFinishConnect() throws Exception;

    protected final ByteBuf newDirectBuffer(ByteBuf buf) {
        final int readableBytes = buf.readableBytes();
        if (readableBytes == 0) {
            ReferenceCountUtil.safeRelease(buf);
            return Unpooled.EMPTY_BUFFER;
        }

        final ByteBufAllocator alloc = alloc();
        if (alloc.isDirectBufferPooled()) {
            ByteBuf directBuf = alloc.directBuffer(readableBytes);
            directBuf.writeBytes(buf, buf.readerIndex(), readableBytes);
            ReferenceCountUtil.safeRelease(buf);
            return directBuf;
        }

        final ByteBuf directBuf = ByteBufUtil.threadLocalDirectBuffer();
        if (directBuf != null) {
            directBuf.writeBytes(buf, buf.readerIndex(), readableBytes);
            ReferenceCountUtil.safeRelease(buf);
            return directBuf;
        }

        return buf;
    }

    protected final ByteBuf newDirectBuffer(ReferenceCounted holder, ByteBuf buf) {
        final int readableBytes = buf.readableBytes();
        if (readableBytes == 0) {
            ReferenceCountUtil.safeRelease(holder);
            return Unpooled.EMPTY_BUFFER;
        }

        final ByteBufAllocator alloc = alloc();
        if (alloc.isDirectBufferPooled()) {
            ByteBuf directBuf = alloc.directBuffer(readableBytes);
            directBuf.writeBytes(buf, buf.readerIndex(), readableBytes);
            ReferenceCountUtil.safeRelease(holder);
            return directBuf;
        }

        final ByteBuf directBuf = ByteBufUtil.threadLocalDirectBuffer();
        if (directBuf != null) {
            directBuf.writeBytes(buf, buf.readerIndex(), readableBytes);
            ReferenceCountUtil.safeRelease(holder);
            return directBuf;
        }

        if (holder != buf) {

            buf.retain();
            ReferenceCountUtil.safeRelease(holder);
        }

        return buf;
    }

    @Override
    protected void doClose() throws Exception {
        // 连接相关的后续处理，并没有实际关闭Channel
        ChannelPromise promise = connectPromise;
        if (promise != null) {
            // 连接操作还在进行，但用户调用close
            promise.tryFailure(DO_CLOSE_CLOSED_CHANNEL_EXCEPTION);
            connectPromise = null;
        }

        ScheduledFuture<?> future = connectTimeoutFuture;
        if (future != null) {
            // 如果有连接超时检测任务，则取消
            future.cancel(false);
            connectTimeoutFuture = null;
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////
    // NioUnsafe
    //////////////////////////////////////////////////////////////////////////////////////////////////

    public interface NioUnsafe extends Unsafe {

        /*
         * 没有connect，不是所有连接都有标准的connect，比如Netty的LocalChannel和EmbeddedChannel
         */

        /**
         * JDK Channel
         */
        SelectableChannel ch();

        /**
         * SelectableChannel设置为非阻塞模式时，connect()立即返回，此时连接可能没完成，要调用JDK的finishConnect()方法完成连接操作
         */
        void finishConnect();

        /**
         * 从JDK Channel中读取数据
         */
        void read();

        void forceFlush();

    }

    //////////////////////////////////////////////////////////////////////////////////////////////////
    // AbstractNioUnsafe
    //////////////////////////////////////////////////////////////////////////////////////////////////

    protected abstract class AbstractNioUnsafe extends AbstractUnsafe implements NioUnsafe {

        protected final void removeReadOp() {
            SelectionKey key = selectionKey();

            if (!key.isValid()) {
                // selectionKey已被取消
                return;
            }

            int interestOps = key.interestOps();
            // Netty中将服务端的OP_ACCEPT和客户端的Read统一抽象为Read事件
            if ((interestOps & readInterestOp) != 0) {
                // 设置为不再感兴趣
                // NIO底层IO事件使用Bitmap表示，一个二进制位对应一个IO事件，该二进制位为1时表示关心该事件，readInterestOp的二进制表示只有1位为1
                key.interestOps(interestOps & ~readInterestOp);
            }
        }

        @Override
        public final SelectableChannel ch() {
            return javaChannel();
        }

        @Override
        public final void connect(final SocketAddress remoteAddress, final SocketAddress localAddress, final ChannelPromise promise) {
            // Channel已被关闭
            if (!promise.setUncancellable() || !ensureOpen(promise)) {
                return;
            }

            try {
                // 已经在连接
                if (connectPromise != null) {
                    throw new ConnectionPendingException();
                }

                boolean wasActive = isActive();
                // 模板方法
                // NioSocketChannel#doConnect
                if (doConnect(remoteAddress, localAddress)) {
                    // 连接已完成，设置异步结果为成功并触发Channel的Active事件
                    fulfillConnectPromise(promise, wasActive);
                }
                // 连接可能未完成
                else {
                    connectPromise = promise;
                    requestedRemoteAddress = remoteAddress;

                    // Netty的连接超时
                    int connectTimeoutMillis = config().getConnectTimeoutMillis();
                    if (connectTimeoutMillis > 0) {
                        // 向EventLoop提交一个调度任务，超时时间已到，则设置异步结果失败然后关闭连接
                        connectTimeoutFuture = eventLoop().schedule(new Runnable() {
                            @Override
                            public void run() {
                                ChannelPromise connectPromise = AbstractNioChannel.this.connectPromise;
                                ConnectTimeoutException cause = new ConnectTimeoutException("connection timed out: " + remoteAddress);
                                if (connectPromise != null && connectPromise.tryFailure(cause)) {
                                    close(voidPromise());
                                }
                            }
                        }, connectTimeoutMillis, TimeUnit.MILLISECONDS);
                    }

                    promise.addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            // 连接操作取消，则连接超时检测任务取消
                            if (future.isCancelled()) {
                                if (connectTimeoutFuture != null) {
                                    connectTimeoutFuture.cancel(false);
                                }
                                connectPromise = null;
                                close(voidPromise());
                            }
                        }
                    });
                }
            } catch (Throwable t) {
                promise.tryFailure(annotateConnectException(t, remoteAddress));
                closeIfClosed();
            }
        }

        private void fulfillConnectPromise(ChannelPromise promise, boolean wasActive) {
            if (promise == null) {
                return;
            }

            boolean active = isActive();

            // false表示取消操作
            boolean promiseSet = promise.trySuccess();

            // 调用前没激活 && 现在已激活
            if (!wasActive && active) {
                // 触发Active事件
                pipeline().fireChannelActive();
            }

            if (!promiseSet) {
                // 操作已被取消，关闭Channel
                close(voidPromise());
            }
        }

        private void fulfillConnectPromise(ChannelPromise promise, Throwable cause) {
            if (promise == null) {
                return;
            }

            promise.tryFailure(cause);
            closeIfClosed();
        }

        @Override
        public final void finishConnect() {
            // 只由EventLoop处理就绪SelectionKey的OP_CONNECT事件时调用，完成连接，连接被取消或超时不会调用
            assert eventLoop().inEventLoop();

            try {
                boolean wasActive = isActive();
                // 模板方法
                doFinishConnect();
                // 首次Active触发Active事件
                fulfillConnectPromise(connectPromise, wasActive);
            } catch (Throwable t) {
                fulfillConnectPromise(connectPromise, annotateConnectException(t, requestedRemoteAddress));
            } finally {

                if (connectTimeoutFuture != null) {
                    // 连接完成，取消超时检测任务
                    connectTimeoutFuture.cancel(false);
                }
                connectPromise = null;
            }
        }

        @Override
        protected final void flush0() {

            if (!isFlushPending()) {
                super.flush0();
            }
        }

        @Override
        public final void forceFlush() {
            // 由EventLoop处理就绪selectionKey的OP_WRITE事件时调用，将缓冲区中的数据写入Channel
            super.flush0();
        }

        private boolean isFlushPending() {
            SelectionKey selectionKey = selectionKey();
            // SelectionKey关心OP_WRITE事件表示正在Flush
            // OP_WRITE表示通道可写，一般通道都可写，如果SelectionKey一直关心OP_WRITE，将不断从select()方法返回
            // Netty使用一个写缓冲区，write操作将数据放入缓冲区中，flush时设置selectionKey关心OP_WRITE事件，完成后取消关心OP_WRITE事件
            // 所以，如果SelectionKey关心OP_WRITE事件表示此时正在Flush数据
            return selectionKey.isValid() && (selectionKey.interestOps() & SelectionKey.OP_WRITE) != 0;
        }
    }
}
