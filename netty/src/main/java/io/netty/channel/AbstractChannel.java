package io.netty.channel;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.socket.ChannelOutputShutdownEvent;
import io.netty.channel.socket.ChannelOutputShutdownException;
import io.netty.util.DefaultAttributeMap;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.ThrowableUtil;
import io.netty.util.internal.UnstableApi;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.IOException;
import java.net.*;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetConnectedException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

public abstract class AbstractChannel extends DefaultAttributeMap implements Channel {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractChannel.class);

    private static final ClosedChannelException ENSURE_OPEN_CLOSED_CHANNEL_EXCEPTION = ThrowableUtil.unknownStackTrace(new ExtendedClosedChannelException(null), AbstractUnsafe.class, "ensureOpen(...)");
    private static final ClosedChannelException CLOSE_CLOSED_CHANNEL_EXCEPTION = ThrowableUtil.unknownStackTrace(new ClosedChannelException(), AbstractUnsafe.class, "close(...)");
    private static final ClosedChannelException WRITE_CLOSED_CHANNEL_EXCEPTION = ThrowableUtil.unknownStackTrace(new ExtendedClosedChannelException(null), AbstractUnsafe.class, "write(...)");
    private static final ClosedChannelException FLUSH0_CLOSED_CHANNEL_EXCEPTION = ThrowableUtil.unknownStackTrace(new ExtendedClosedChannelException(null), AbstractUnsafe.class, "flush0()");
    private static final NotYetConnectedException FLUSH0_NOT_YET_CONNECTED_EXCEPTION = ThrowableUtil.unknownStackTrace(new NotYetConnectedException(), AbstractUnsafe.class, "flush0()");
    /**
     * 父Channel
     */
    private final Channel parent;
    private final ChannelId id;
    /**
     * Unsafe
     */
    private final Unsafe unsafe;
    /**
     * Pipeline
     */
    private final DefaultChannelPipeline pipeline;
    private final VoidChannelPromise unsafeVoidPromise = new VoidChannelPromise(this, false);
    private final CloseFuture closeFuture = new CloseFuture(this);
    private volatile SocketAddress localAddress;
    private volatile SocketAddress remoteAddress;
    /**
     * 绑定的EventLoop
     */
    private volatile EventLoop eventLoop;
    /**
     * 是否注册到EventLoop
     */
    private volatile boolean registered;
    private boolean closeInitiated;
    private Throwable initialCloseCause;
    private boolean strValActive;
    private String strVal;

    /**
     * 构造方法
     */
    protected AbstractChannel(Channel parent) {
        this.parent = parent;
        id = newId();
        unsafe = newUnsafe();
        pipeline = newChannelPipeline();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////
    // whatever
    //////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public final ChannelId id() {
        return id;
    }

    protected ChannelId newId() {
        return DefaultChannelId.newInstance();
    }

    @Override
    public ChannelPipeline pipeline() {
        return pipeline;
    }

    protected DefaultChannelPipeline newChannelPipeline() {
        return new DefaultChannelPipeline(this);
    }

    @Override
    public Unsafe unsafe() {
        return unsafe;
    }

    protected abstract AbstractUnsafe newUnsafe();

    @Override
    public Channel parent() {
        return parent;
    }

    @Override
    public ByteBufAllocator alloc() {
        return config().getAllocator();
    }

    @Override
    public EventLoop eventLoop() {
        EventLoop eventLoop = this.eventLoop;
        if (eventLoop == null) {
            throw new IllegalStateException("channel not registered to an event loop");
        }
        return eventLoop;
    }

    @Override
    public SocketAddress localAddress() {
        SocketAddress localAddress = this.localAddress;
        if (localAddress == null) {
            try {
                this.localAddress = localAddress = unsafe().localAddress();
            } catch (Error e) {
                throw e;
            } catch (Throwable t) {

                return null;
            }
        }
        return localAddress;
    }

    @Deprecated
    protected void invalidateLocalAddress() {
        localAddress = null;
    }

    @Override
    public SocketAddress remoteAddress() {
        SocketAddress remoteAddress = this.remoteAddress;
        if (remoteAddress == null) {
            try {
                this.remoteAddress = remoteAddress = unsafe().remoteAddress();
            } catch (Error e) {
                throw e;
            } catch (Throwable t) {

                return null;
            }
        }
        return remoteAddress;
    }

    @Deprecated
    protected void invalidateRemoteAddress() {
        remoteAddress = null;
    }

    @Override
    public boolean isRegistered() {
        return registered;
    }

    @Override
    public ChannelFuture closeFuture() {
        return closeFuture;
    }

    protected abstract boolean isCompatible(EventLoop loop);

    protected abstract SocketAddress localAddress0();

    protected abstract SocketAddress remoteAddress0();

    @Override
    public boolean isWritable() {
        ChannelOutboundBuffer buf = unsafe.outboundBuffer();
        return buf != null && buf.isWritable();
    }

    @Override
    public long bytesBeforeUnwritable() {
        ChannelOutboundBuffer buf = unsafe.outboundBuffer();

        return buf != null ? buf.bytesBeforeUnwritable() : 0;
    }

    @Override
    public long bytesBeforeWritable() {
        ChannelOutboundBuffer buf = unsafe.outboundBuffer();

        return buf != null ? buf.bytesBeforeWritable() : Long.MAX_VALUE;
    }

    @Override
    public final int hashCode() {
        return id.hashCode();
    }

    @Override
    public final boolean equals(Object o) {
        return this == o;
    }

    @Override
    public final int compareTo(Channel o) {
        if (this == o) {
            return 0;
        }

        return id().compareTo(o.id());
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////
    // Pipeline
    //////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public ChannelFuture bind(SocketAddress localAddress) {
        return pipeline.bind(localAddress);
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise) {
        return pipeline.bind(localAddress, promise);
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress) {
        return pipeline.connect(remoteAddress);
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress) {
        return pipeline.connect(remoteAddress, localAddress);
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise) {
        return pipeline.connect(remoteAddress, promise);
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
        return pipeline.connect(remoteAddress, localAddress, promise);
    }

    @Override
    public ChannelFuture disconnect() {
        return pipeline.disconnect();
    }

    @Override
    public ChannelFuture disconnect(ChannelPromise promise) {
        return pipeline.disconnect(promise);
    }

    @Override
    public ChannelFuture close() {
        return pipeline.close();
    }

    @Override
    public ChannelFuture close(ChannelPromise promise) {
        return pipeline.close(promise);
    }

    @Override
    public ChannelFuture deregister() {
        return pipeline.deregister();
    }

    @Override
    public ChannelFuture deregister(ChannelPromise promise) {
        return pipeline.deregister(promise);
    }

    @Override
    public Channel read() {
        pipeline.read();
        return this;
    }

    @Override
    public ChannelFuture write(Object msg) {
        return pipeline.write(msg);
    }

    @Override
    public ChannelFuture write(Object msg, ChannelPromise promise) {
        return pipeline.write(msg, promise);
    }

    @Override
    public Channel flush() {
        pipeline.flush();
        return this;
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg) {
        return pipeline.writeAndFlush(msg);
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
        return pipeline.writeAndFlush(msg, promise);
    }

    @Override
    public ChannelPromise newPromise() {
        return pipeline.newPromise();
    }

    @Override
    public ChannelProgressivePromise newProgressivePromise() {
        return pipeline.newProgressivePromise();
    }

    @Override
    public ChannelFuture newSucceededFuture() {
        return pipeline.newSucceededFuture();
    }

    @Override
    public ChannelFuture newFailedFuture(Throwable cause) {
        return pipeline.newFailedFuture(cause);
    }

    @Override
    public final ChannelPromise voidPromise() {
        return pipeline.voidPromise();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////
    // do
    //////////////////////////////////////////////////////////////////////////////////////////////////

    protected void doRegister() throws Exception {

    }

    protected abstract void doBind(SocketAddress localAddress) throws Exception;

    protected abstract void doDisconnect() throws Exception;

    protected abstract void doClose() throws Exception;

    @UnstableApi
    protected void doShutdownOutput() throws Exception {
        doClose();
    }

    protected void doDeregister() throws Exception {

    }

    protected abstract void doBeginRead() throws Exception;

    protected abstract void doWrite(ChannelOutboundBuffer in) throws Exception;

    protected Object filterOutboundMessage(Object msg) throws Exception {
        return msg;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////
    // CloseFuture
    //////////////////////////////////////////////////////////////////////////////////////////////////

    static final class CloseFuture extends DefaultChannelPromise {

        CloseFuture(AbstractChannel ch) {
            super(ch);
        }

        @Override
        public ChannelPromise setSuccess() {
            throw new IllegalStateException();
        }

        @Override
        public ChannelPromise setFailure(Throwable cause) {
            throw new IllegalStateException();
        }

        @Override
        public boolean trySuccess() {
            throw new IllegalStateException();
        }

        @Override
        public boolean tryFailure(Throwable cause) {
            throw new IllegalStateException();
        }

        boolean setClosed() {
            return super.trySuccess();
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////
    // AnnotatedConnectException
    //////////////////////////////////////////////////////////////////////////////////////////////////

    private static final class AnnotatedConnectException extends ConnectException {

        private static final long serialVersionUID = 3901958112696433556L;

        AnnotatedConnectException(ConnectException exception, SocketAddress remoteAddress) {
            super(exception.getMessage() + ": " + remoteAddress);
            initCause(exception);
            setStackTrace(exception.getStackTrace());
        }

        @Override
        public Throwable fillInStackTrace() {
            return this;
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////
    // AnnotatedNoRouteToHostException
    //////////////////////////////////////////////////////////////////////////////////////////////////

    private static final class AnnotatedNoRouteToHostException extends NoRouteToHostException {

        private static final long serialVersionUID = -6801433937592080623L;

        AnnotatedNoRouteToHostException(NoRouteToHostException exception, SocketAddress remoteAddress) {
            super(exception.getMessage() + ": " + remoteAddress);
            initCause(exception);
            setStackTrace(exception.getStackTrace());
        }

        @Override
        public Throwable fillInStackTrace() {
            return this;
        }
    }
    //////////////////////////////////////////////////////////////////////////////////////////////////
    // AnnotatedSocketException
    //////////////////////////////////////////////////////////////////////////////////////////////////

    private static final class AnnotatedSocketException extends SocketException {

        private static final long serialVersionUID = 3896743275010454039L;

        AnnotatedSocketException(SocketException exception, SocketAddress remoteAddress) {
            super(exception.getMessage() + ": " + remoteAddress);
            initCause(exception);
            setStackTrace(exception.getStackTrace());
        }

        @Override
        public Throwable fillInStackTrace() {
            return this;
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////
    // AbstractUnsafe
    //////////////////////////////////////////////////////////////////////////////////////////////////

    protected abstract class AbstractUnsafe implements Unsafe {

        /**
         * 写缓冲区
         */
        private volatile ChannelOutboundBuffer outboundBuffer = new ChannelOutboundBuffer(AbstractChannel.this);
        private RecvByteBufAllocator.Handle recvHandle;
        private boolean inFlush0;

        private boolean neverRegistered = true;

        private void assertEventLoop() {
            assert !registered || eventLoop.inEventLoop();
        }

        @Override
        public RecvByteBufAllocator.Handle recvBufAllocHandle() {
            if (recvHandle == null) {
                recvHandle = config().getRecvByteBufAllocator().newHandle();
            }
            return recvHandle;
        }

        @Override
        public final ChannelOutboundBuffer outboundBuffer() {
            return outboundBuffer;
        }

        @Override
        public final SocketAddress localAddress() {
            return localAddress0();
        }

        @Override
        public final SocketAddress remoteAddress() {
            return remoteAddress0();
        }

        /**
         * 注册
         */
        @Override
        public final void register(EventLoop eventLoop, final ChannelPromise promise) {
            if (eventLoop == null) {
                throw new NullPointerException("eventLoop");
            }
            if (isRegistered()) {
                // 已经注册则失败
                promise.setFailure(new IllegalStateException("registered to an event loop already"));
                return;
            }
            // EventLoop不兼容当前Channel
            if (!isCompatible(eventLoop)) {
                promise.setFailure(new IllegalStateException("incompatible event loop type: " + eventLoop.getClass().getName()));
                return;
            }
            // 注册Channel到EventLoop
            AbstractChannel.this.eventLoop = eventLoop;
            // 当前线程为EventLoop线程直接执行，比如unregister再register
            if (eventLoop.inEventLoop()) {
                register0(promise);
            }
            // 否则提交任务给EventLoop线程执行（异步）
            else {
                try {
                    // 由Channel绑定的EventLoop执行，这里会启动EventLoop线程
                    eventLoop.execute(new Runnable() {
                        @Override
                        public void run() {
                            register0(promise);
                        }
                    });
                } catch (Throwable t) {
                    logger.warn("Force-closing a channel whose registration task was not accepted by an event loop: {}", AbstractChannel.this, t);
                    // 异常时关闭Channel
                    closeForcibly();
                    closeFuture.setClosed();
                    safeSetFailure(promise, t);
                }
            }
        }

        private void register0(ChannelPromise promise) {
            try {

                // 确保Channel没有关闭
                if (!promise.setUncancellable() || !ensureOpen(promise)) {
                    return;
                }
                boolean firstRegistration = neverRegistered;
                // JDK Channel注册到Selector，模板方法
                // AbstractNioChannel#doRegister
                doRegister();
                neverRegistered = false;
                registered = true;
                // 完成ChannelInitializer的回调，执行initChannel，将处理业务逻辑的用户Handler添加到ChannelPipeline
                pipeline.invokeHandlerAddedIfNeeded();
                // 设置异步注册成功，触发回调
                safeSetSuccess(promise);
                // 传播Channel注册事件，head开始
                pipeline.fireChannelRegistered();

                // 服务端ServerSocketChannel接受的客户端Channel此时已被激活
                if (isActive()) {
                    if (firstRegistration) {
                        // 首次注册且激活触发Channel激活事件
                        pipeline.fireChannelActive();
                    }
                    // 已设置autoRead
                    else if (config().isAutoRead()) {
                        // 读取数据，模板方法
                        beginRead();
                    }
                }
            } catch (Throwable t) {
                // 模板方法
                closeForcibly();
                closeFuture.setClosed();
                safeSetFailure(promise, t);
            }
        }

        @Override
        public final void bind(final SocketAddress localAddress, final ChannelPromise promise) {
            assertEventLoop();

            if (!promise.setUncancellable() || !ensureOpen(promise)) {
                return;
            }

            if (Boolean.TRUE.equals(config().getOption(ChannelOption.SO_BROADCAST)) && localAddress instanceof InetSocketAddress && !((InetSocketAddress) localAddress).getAddress().isAnyLocalAddress() && !PlatformDependent.isWindows() && !PlatformDependent.maybeSuperUser()) {

                logger.warn("A non-root user can't receive a broadcast packet if the socket " + "is not bound to a wildcard address; binding to a non-wildcard " + "address (" + localAddress + ") anyway as requested.");
            }

            boolean wasActive = isActive();
            try {
                // 模板方法，JDK Channel绑定端口
                // NioServerSocketChannel#doBind
                doBind(localAddress);
            } catch (Throwable t) {
                safeSetFailure(promise, t);
                // 当Channel不再打开时关闭Channel
                closeIfClosed();
                return;
            }
            // Channel绑定完成后触发Active事件
            if (!wasActive && isActive()) {
                // 向Channel注册到的EventLoop提交一个任务
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        pipeline.fireChannelActive();
                    }
                });
            }
            // 设置回调
            safeSetSuccess(promise);
        }

        @Override
        public final void disconnect(final ChannelPromise promise) {
            assertEventLoop();

            if (!promise.setUncancellable()) {
                return;
            }

            boolean wasActive = isActive();
            try {
                // 模板方法
                doDisconnect();
            } catch (Throwable t) {
                safeSetFailure(promise, t);
                closeIfClosed();
                return;
            }

            if (wasActive && !isActive()) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        pipeline.fireChannelInactive();
                    }
                });
            }

            safeSetSuccess(promise);
            closeIfClosed();
        }

        @Override
        public final void close(final ChannelPromise promise) {
            assertEventLoop();

            close(promise, CLOSE_CLOSED_CHANNEL_EXCEPTION, CLOSE_CLOSED_CHANNEL_EXCEPTION, false);
        }

        private void close(final ChannelPromise promise, final Throwable cause, final ClosedChannelException closeCause, final boolean notify) {
            if (!promise.setUncancellable()) {
                return;
            }

            if (closeInitiated) {
                if (closeFuture.isDone()) {
                    // 已经关闭，保证底层close只执行一次
                    safeSetSuccess(promise);
                } else if (!(promise instanceof VoidChannelPromise)) {
                    // 当Channel关闭时，将此次close异步请求结果也设置为成功

                    closeFuture.addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            promise.setSuccess();
                        }
                    });
                }
                return;
            }

            closeInitiated = true;

            final boolean wasActive = isActive();
            final ChannelOutboundBuffer outboundBuffer = this.outboundBuffer;
            // outboundBuffer作为一个标记，为空表示Channel正在关闭，此时禁止写操作
            // 设置为空禁止write操作，同时作为标记字段表示正在关闭
            this.outboundBuffer = null;
            // 关闭前的清除工作并返回一个Executor
            // 如果不为空，需要在该Executor里执行doClose0()方法；为空，则在当前线程执行
            Executor closeExecutor = prepareToClose();
            if (closeExecutor != null) {
                closeExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // prepareToClose返回的executor执行
                            doClose0(promise);
                        } finally {
                            // Channel注册的EventLoop执行
                            invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    if (outboundBuffer != null) {
                                        // 写缓冲队列中的数据全部设置失败
                                        outboundBuffer.failFlushed(cause, notify);
                                        outboundBuffer.close(closeCause);
                                    }
                                    // 需要invokeLater()使用EventLoop执行
                                    // 其中会调用deRegister()方法触发Inactive事件，而事件执行需要在EventLoop中执行
                                    fireChannelInactiveAndDeregister(wasActive);
                                }
                            });
                        }
                    }
                });
            }

            // 当前调用线程执行
            else {
                try {

                    doClose0(promise);
                } finally {
                    if (outboundBuffer != null) {

                        outboundBuffer.failFlushed(cause, notify);
                        outboundBuffer.close(closeCause);
                    }
                }
                if (inFlush0) {
                    invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            fireChannelInactiveAndDeregister(wasActive);
                        }
                    });
                } else {
                    fireChannelInactiveAndDeregister(wasActive);
                }
            }
        }

        private void doClose0(ChannelPromise promise) {
            try {
                // 模板方法
                doClose();
                closeFuture.setClosed();
                safeSetSuccess(promise);
            } catch (Throwable t) {
                closeFuture.setClosed();
                safeSetFailure(promise, t);
            }
        }

        @Override
        public final void closeForcibly() {
            assertEventLoop();

            try {
                doClose();
            } catch (Exception e) {
                logger.warn("Failed to close a channel.", e);
            }
        }

        @UnstableApi
        public final void shutdownOutput(final ChannelPromise promise) {
            assertEventLoop();
            shutdownOutput(promise, null);
        }

        private void shutdownOutput(final ChannelPromise promise, Throwable cause) {
            if (!promise.setUncancellable()) {
                return;
            }

            final ChannelOutboundBuffer outboundBuffer = this.outboundBuffer;
            if (outboundBuffer == null) {
                promise.setFailure(CLOSE_CLOSED_CHANNEL_EXCEPTION);
                return;
            }
            this.outboundBuffer = null;

            final Throwable shutdownCause = cause == null ? new ChannelOutputShutdownException("Channel output shutdown") : new ChannelOutputShutdownException("Channel output shutdown", cause);
            Executor closeExecutor = prepareToClose();
            if (closeExecutor != null) {
                closeExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {

                            doShutdownOutput();
                            promise.setSuccess();
                        } catch (Throwable err) {
                            promise.setFailure(err);
                        } finally {

                            eventLoop().execute(new Runnable() {
                                @Override
                                public void run() {
                                    closeOutboundBufferForShutdown(pipeline, outboundBuffer, shutdownCause);
                                }
                            });
                        }
                    }
                });
            } else {
                try {

                    doShutdownOutput();
                    promise.setSuccess();
                } catch (Throwable err) {
                    promise.setFailure(err);
                } finally {
                    closeOutboundBufferForShutdown(pipeline, outboundBuffer, shutdownCause);
                }
            }
        }

        private void closeOutboundBufferForShutdown(ChannelPipeline pipeline, ChannelOutboundBuffer buffer, Throwable cause) {
            buffer.failFlushed(cause, false);
            buffer.close(cause, true);
            pipeline.fireUserEventTriggered(ChannelOutputShutdownEvent.INSTANCE);
        }

        private void fireChannelInactiveAndDeregister(final boolean wasActive) {
            deregister(voidPromise(), wasActive && !isActive());
        }

        @Override
        public final void deregister(final ChannelPromise promise) {
            assertEventLoop();

            deregister(promise, false);
        }

        private void deregister(final ChannelPromise promise, final boolean fireChannelInactive) {
            if (!promise.setUncancellable()) {
                return;
            }

            if (!registered) {
                // 已经deregister
                safeSetSuccess(promise);
                return;
            }

            // 用户可能会在ChannelPipeline中将当前Channel注册到新的EventLoop，确保ChannelPipeline事件和doDeregister()在同一个EventLoop完成？
            invokeLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        // 模板方法
                        doDeregister();
                    } catch (Throwable t) {
                        logger.warn("Unexpected exception occurred while deregistering a channel.", t);
                    } finally {
                        if (fireChannelInactive) {
                            // 根据参数触发Inactive事件
                            pipeline.fireChannelInactive();
                        }

                        if (registered) {
                            registered = false;
                            // 首次调用触发Unregistered事件
                            pipeline.fireChannelUnregistered();
                        }
                        safeSetSuccess(promise);
                    }
                }
            });
        }

        @Override
        public final void beginRead() {
            assertEventLoop();

            if (!isActive()) {
                return;
            }

            try {
                // AbstractNioChannel#doBeginRead
                // AbstractNioMessageChannel#doBeginRead
                doBeginRead();
            } catch (final Exception e) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        pipeline.fireExceptionCaught(e);
                    }
                });
                close(voidPromise());
            }
        }

        @Override
        public final void write(Object msg, ChannelPromise promise) {
            assertEventLoop();

            ChannelOutboundBuffer outboundBuffer = this.outboundBuffer;
            if (outboundBuffer == null) {
                // 见close，outboundBuffer为空表示Channel正在关闭，禁止写数据
                safeSetFailure(promise, newWriteException(initialCloseCause));
                // 释放
                ReferenceCountUtil.release(msg);
                return;
            }

            int size;
            try {
                // 可对消息进行过滤整理，例如把HeapBuffer转为DirectBuffer，具体实现由子类负责
                msg = filterOutboundMessage(msg);
                size = pipeline.estimatorHandle().size(msg);
                if (size < 0) {
                    size = 0;
                }
            } catch (Throwable t) {
                safeSetFailure(promise, t);
                ReferenceCountUtil.release(msg);
                return;
            }

            outboundBuffer.addMessage(msg, size, promise);
        }

        @Override
        public final void flush() {
            assertEventLoop();
            // 引入了一个写缓冲区ChannelOutboundBuffer，由该缓冲区控制Channel的可写状态
            ChannelOutboundBuffer outboundBuffer = this.outboundBuffer;
            if (outboundBuffer == null) {
                // Channel正在关闭直接返回
                return;
            }
            outboundBuffer.addFlush();
            // 执行真正的底层写操作
            flush0();
        }

        @SuppressWarnings("deprecation")
        protected void flush0() {
            // 正在flush
            if (inFlush0) {
                return;
            }

            final ChannelOutboundBuffer outboundBuffer = this.outboundBuffer;
            // Channel正在关闭或者已没有需要写的数据
            if (outboundBuffer == null || outboundBuffer.isEmpty()) {
                return;
            }

            inFlush0 = true;

            if (!isActive()) {
                // Channel已经非激活，将所有进行中的写请求标记为失败
                try {
                    if (isOpen()) {
                        outboundBuffer.failFlushed(FLUSH0_NOT_YET_CONNECTED_EXCEPTION, true);
                    } else {

                        outboundBuffer.failFlushed(newFlush0Exception(initialCloseCause), false);
                    }
                } finally {
                    inFlush0 = false;
                }
                return;
            }

            try {
                // 模板方法
                // AbstractNioByteChannel#doWrite
                // AbstractNioMessageChannel#doWrite
                doWrite(outboundBuffer);
            } catch (Throwable t) {
                if (t instanceof IOException && config().isAutoClose()) {

                    initialCloseCause = t;
                    close(voidPromise(), t, newFlush0Exception(t), false);
                } else {
                    try {
                        shutdownOutput(voidPromise(), t);
                    } catch (Throwable t2) {
                        initialCloseCause = t;
                        close(voidPromise(), t2, newFlush0Exception(t), false);
                    }
                }
            } finally {
                inFlush0 = false;
            }
        }

        @Override
        public final ChannelPromise voidPromise() {
            assertEventLoop();

            return unsafeVoidPromise;
        }

        protected final boolean ensureOpen(ChannelPromise promise) {
            if (isOpen()) {
                return true;
            }

            safeSetFailure(promise, newEnsureOpenException(initialCloseCause));
            return false;
        }

        protected final void safeSetSuccess(ChannelPromise promise) {
            if (!(promise instanceof VoidChannelPromise) && !promise.trySuccess()) {
                logger.warn("Failed to mark a promise as success because it is done already: {}", promise);
            }
        }

        protected final void safeSetFailure(ChannelPromise promise, Throwable cause) {
            if (!(promise instanceof VoidChannelPromise) && !promise.tryFailure(cause)) {
                logger.warn("Failed to mark a promise as failure because it's done already: {}", promise, cause);
            }
        }

        protected final void closeIfClosed() {
            if (isOpen()) {
                return;
            }
            close(voidPromise());
        }

        private void invokeLater(Runnable task) {
            try {
                eventLoop().execute(task);
            } catch (RejectedExecutionException e) {
                logger.warn("Can't invoke task later as EventLoop rejected it", e);
            }
        }

        protected final Throwable annotateConnectException(Throwable cause, SocketAddress remoteAddress) {
            if (cause instanceof ConnectException) {
                return new AnnotatedConnectException((ConnectException) cause, remoteAddress);
            }
            if (cause instanceof NoRouteToHostException) {
                return new AnnotatedNoRouteToHostException((NoRouteToHostException) cause, remoteAddress);
            }
            if (cause instanceof SocketException) {
                return new AnnotatedSocketException((SocketException) cause, remoteAddress);
            }

            return cause;
        }

        private ClosedChannelException newWriteException(Throwable cause) {
            if (cause == null) {
                return WRITE_CLOSED_CHANNEL_EXCEPTION;
            }
            return ThrowableUtil.unknownStackTrace(new ExtendedClosedChannelException(cause), AbstractUnsafe.class, "write(...)");
        }

        private ClosedChannelException newFlush0Exception(Throwable cause) {
            if (cause == null) {
                return FLUSH0_CLOSED_CHANNEL_EXCEPTION;
            }
            return ThrowableUtil.unknownStackTrace(new ExtendedClosedChannelException(cause), AbstractUnsafe.class, "flush0()");
        }

        private ClosedChannelException newEnsureOpenException(Throwable cause) {
            if (cause == null) {
                return ENSURE_OPEN_CLOSED_CHANNEL_EXCEPTION;
            }
            return ThrowableUtil.unknownStackTrace(new ExtendedClosedChannelException(cause), AbstractUnsafe.class, "ensureOpen(...)");
        }

        protected Executor prepareToClose() {
            return null;
        }
    }
}
