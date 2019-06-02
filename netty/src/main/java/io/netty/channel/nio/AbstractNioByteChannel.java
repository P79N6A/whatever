package io.netty.channel.nio;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.ChannelInputShutdownEvent;
import io.netty.channel.socket.ChannelInputShutdownReadComplete;
import io.netty.channel.socket.SocketChannelConfig;
import io.netty.util.internal.StringUtil;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

import static io.netty.channel.internal.ChannelUtils.WRITE_STATUS_SNDBUF_FULL;

/**
 * 客户端Channel，AbstractNioByteChannel每读取到一定字节就触发ChannelRead事件（响应快）
 */
public abstract class AbstractNioByteChannel extends AbstractNioChannel {

    private static final ChannelMetadata METADATA = new ChannelMetadata(false, 16);
    private static final String EXPECTED_TYPES = " (expected: " + StringUtil.simpleClassName(ByteBuf.class) + ", " + StringUtil.simpleClassName(FileRegion.class) + ')';

    private final Runnable flushTask = new Runnable() {
        @Override
        public void run() {
            ((AbstractNioUnsafe) unsafe()).flush0();
        }
    };
    private boolean inputClosedSeenErrorOnRead;

    /**
     * 构造方法
     */
    protected AbstractNioByteChannel(Channel parent, SelectableChannel ch) {
        // OP_READ
        super(parent, ch, SelectionKey.OP_READ);
    }
    //////////////////////////////////////////////////////////////////////////////////////////////////
    // whatever
    //////////////////////////////////////////////////////////////////////////////////////////////////

    private static boolean isAllowHalfClosure(ChannelConfig config) {
        return config instanceof SocketChannelConfig && ((SocketChannelConfig) config).isAllowHalfClosure();
    }

    protected abstract ChannelFuture shutdownInput();

    protected boolean isInputShutdown0() {
        return false;
    }

    final boolean shouldBreakReadReady(ChannelConfig config) {
        return isInputShutdown0() && (inputClosedSeenErrorOnRead || !isAllowHalfClosure(config));
    }

    @Override
    protected AbstractNioUnsafe newUnsafe() {
        return new NioByteUnsafe();
    }

    @Override
    public ChannelMetadata metadata() {
        return METADATA;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////
    // do
    //////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void doWrite(ChannelOutboundBuffer in) throws Exception {
        int writeSpinCount = config().getWriteSpinCount();
        do {
            Object msg = in.current();
            // 数据已全部写完
            if (msg == null) {
                // 清除OP_WRITE事件
                clearOpWrite();

                return;
            }

            writeSpinCount -= doWriteInternal(in, msg);
        } while (writeSpinCount > 0);
        // 将ByteBuf的数据写入Channel，NIO写操作返回已写入的数据量，非阻塞模式下可能为0，此时调用incompleteWrite()方法
        incompleteWrite(writeSpinCount < 0);
    }

    protected final int doWrite0(ChannelOutboundBuffer in) throws Exception {
        Object msg = in.current();
        if (msg == null) {

            return 0;
        }
        return doWriteInternal(in, in.current());
    }

    private int doWriteInternal(ChannelOutboundBuffer in, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            ByteBuf buf = (ByteBuf) msg;
            // ByteBuf不可读，此时数据已写完
            if (!buf.isReadable()) {
                // 完成时，清理缓冲区
                in.remove();
                return 0;
            }
            // 模板方法
            final int localFlushedAmount = doWriteBytes(buf);
            // 返回已写入的数据量，NIO在非阻塞模式下写操作可能返回0表示未写入数据
            if (localFlushedAmount > 0) {
                in.progress(localFlushedAmount);
                if (!buf.isReadable()) {
                    in.remove();
                }
                return 1;
            }
        }

        // FileRegion是Netty对NIO底层的FileChannel的封装，负责将File中的数据写入到WritableChannel中
        else if (msg instanceof FileRegion) {
            FileRegion region = (FileRegion) msg;
            if (region.transferred() >= region.count()) {
                in.remove();
                return 0;
            }

            long localFlushedAmount = doWriteFileRegion(region);
            if (localFlushedAmount > 0) {
                in.progress(localFlushedAmount);
                if (region.transferred() >= region.count()) {
                    in.remove();
                }
                return 1;
            }
        } else {
            // 其他类型不支持
            throw new Error();
        }
        return WRITE_STATUS_SNDBUF_FULL;
    }

    protected abstract long doWriteFileRegion(FileRegion region) throws Exception;

    protected abstract int doReadBytes(ByteBuf buf) throws Exception;

    protected abstract int doWriteBytes(ByteBuf buf) throws Exception;

    @Override
    protected final Object filterOutboundMessage(Object msg) {
        // 过滤要写的数据
        if (msg instanceof ByteBuf) {
            ByteBuf buf = (ByteBuf) msg;
            if (buf.isDirect()) {
                return msg;
            }
            // 非DirectBuf转为DirectBuf
            return newDirectBuffer(buf);
        }
        // 写数据类型只支持DirectBuffer和FileRegion
        if (msg instanceof FileRegion) {
            return msg;
        }

        throw new UnsupportedOperationException("unsupported message type: " + StringUtil.simpleClassName(msg) + EXPECTED_TYPES);
    }

    protected final void incompleteWrite(boolean setOpWrite) {
        // 未写入数据：设置SelectionKey继续关心OP_WRITE事件，继续进行写操作
        if (setOpWrite) {
            // 设置继续关心OP_WRITE事件
            setOpWrite();
        }
        // 写操作次数达到writeSpinCount但没写完
        else {
            // 清除
            clearOpWrite();
            // 向EventLoop提交一个新的flush任务，此时可以响应其他请求，不会占用全部资源使其他请求得不到响应
            eventLoop().execute(flushTask);
        }
    }

    protected final void setOpWrite() {
        final SelectionKey key = selectionKey();

        if (!key.isValid()) {
            return;
        }
        final int interestOps = key.interestOps();
        if ((interestOps & SelectionKey.OP_WRITE) == 0) {
            key.interestOps(interestOps | SelectionKey.OP_WRITE);
        }
    }

    protected final void clearOpWrite() {
        final SelectionKey key = selectionKey();

        if (!key.isValid()) {
            return;
        }
        final int interestOps = key.interestOps();
        if ((interestOps & SelectionKey.OP_WRITE) != 0) {
            key.interestOps(interestOps & ~SelectionKey.OP_WRITE);
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////
    // NioByteUnsafe
    //////////////////////////////////////////////////////////////////////////////////////////////////

    protected class NioByteUnsafe extends AbstractNioUnsafe {

        private void closeOnRead(ChannelPipeline pipeline) {
            if (!isInputShutdown0()) {
                if (isAllowHalfClosure(config())) {
                    // 远端关闭此时设置Channel的输入源关闭
                    shutdownInput();
                    // 触发用户事件ChannelInputShutdownEvent
                    pipeline.fireUserEventTriggered(ChannelInputShutdownEvent.INSTANCE);
                } else {
                    // 直接关闭Channel
                    close(voidPromise());
                }
            } else {
                inputClosedSeenErrorOnRead = true;
                pipeline.fireUserEventTriggered(ChannelInputShutdownReadComplete.INSTANCE);
            }
        }

        private void handleReadException(ChannelPipeline pipeline, ByteBuf byteBuf, Throwable cause, boolean close, RecvByteBufAllocator.Handle allocHandle) {

            // 已读取到数据
            if (byteBuf != null) {
                // 数据可读
                if (byteBuf.isReadable()) {
                    readPending = false;
                    pipeline.fireChannelRead(byteBuf);
                }
                // 不可读
                else {
                    byteBuf.release();
                }
            }
            allocHandle.readComplete();
            pipeline.fireChannelReadComplete();
            pipeline.fireExceptionCaught(cause);
            if (close || cause instanceof IOException) {
                closeOnRead(pipeline);
            }
        }

        @Override
        public final void read() {
            final ChannelConfig config = config();
            if (shouldBreakReadReady(config)) {
                clearReadPending();
                return;
            }
            final ChannelPipeline pipeline = pipeline();
            final ByteBufAllocator allocator = config.getAllocator();
            final RecvByteBufAllocator.Handle allocHandle = recvBufAllocHandle();
            allocHandle.reset(config);

            ByteBuf byteBuf = null;
            boolean close = false;
            try {
                do {
                    // 创建一个ByteBuf
                    byteBuf = allocHandle.allocate(allocator);
                    // 模板方法
                    allocHandle.lastBytesRead(doReadBytes(byteBuf));
                    // 没有数据可读
                    if (allocHandle.lastBytesRead() <= 0) {

                        byteBuf.release();
                        byteBuf = null;
                        // 读取数据量为负，对端已关闭
                        close = allocHandle.lastBytesRead() < 0;
                        if (close) {
                            readPending = false;
                        }
                        break;
                    }

                    allocHandle.incMessagesRead(1);
                    readPending = false;
                    // 触发ChannelRead事件，用户处理
                    pipeline.fireChannelRead(byteBuf);
                    byteBuf = null;
                } while (allocHandle.continueReading());

                allocHandle.readComplete();
                // ReadComplete结束时，如果开启autoRead则会调用beginRead，继续read
                pipeline.fireChannelReadComplete();

                if (close) {
                    closeOnRead(pipeline);
                }
            } catch (Throwable t) {
                handleReadException(pipeline, byteBuf, t, close, allocHandle);
            } finally {

                // 读事件没在进行 && 没有配置autoRead
                if (!readPending && !config.isAutoRead()) {
                    // 移除
                    removeReadOp();
                }
            }
        }
    }
}
