package io.netty.channel.nio;

import io.netty.channel.*;

import java.io.IOException;
import java.net.PortUnreachableException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.List;

/**
 * 服务端Channel，AbstractNioMessageChannel依次读取Message，最后统一触发ChannelRead事件（高吞吐量，ServerSocketChannel需要尽可能多地接受连接）
 */
public abstract class AbstractNioMessageChannel extends AbstractNioChannel {

    boolean inputShutdown;

    protected AbstractNioMessageChannel(Channel parent, SelectableChannel ch, int readInterestOp) {
        super(parent, ch, readInterestOp);
    }

    @Override
    protected AbstractNioUnsafe newUnsafe() {
        return new NioMessageUnsafe();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////
    // do
    //////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void doBeginRead() throws Exception {
        if (inputShutdown) {
            return;
        }
        super.doBeginRead();
    }

    @Override
    protected void doWrite(ChannelOutboundBuffer in) throws Exception {
        final SelectionKey key = selectionKey();
        final int interestOps = key.interestOps();

        for (; ; ) {
            Object msg = in.current();
            if (msg == null) {

                // key.isWritable()
                if ((interestOps & SelectionKey.OP_WRITE) != 0) {
                    // 取消关心
                    key.interestOps(interestOps & ~SelectionKey.OP_WRITE);
                }
                break;
            }
            try {
                boolean done = false;
                for (int i = config().getWriteSpinCount() - 1; i >= 0; i--) {
                    // ServerChannel一般是不写的，不会关心这个事件
                    if (doWriteMessage(msg, in)) {
                        done = true;
                        break;
                    }
                }

                if (done) {
                    in.remove();
                } else {

                    if ((interestOps & SelectionKey.OP_WRITE) == 0) {
                        key.interestOps(interestOps | SelectionKey.OP_WRITE);
                    }
                    break;
                }
            } catch (Exception e) {
                if (continueOnWriteError()) {
                    in.remove(e);
                } else {
                    throw e;
                }
            }
        }
    }

    protected boolean continueOnWriteError() {
        return false;
    }

    protected boolean closeOnReadError(Throwable cause) {
        if (!isActive()) {
            return true;
        }
        if (cause instanceof PortUnreachableException) {
            return false;
        }
        if (cause instanceof IOException) {

            return !(this instanceof ServerChannel);
        }
        return true;
    }

    protected abstract int doReadMessages(List<Object> buf) throws Exception;

    protected abstract boolean doWriteMessage(Object msg, ChannelOutboundBuffer in) throws Exception;

    //////////////////////////////////////////////////////////////////////////////////////////////////
    // NioMessageUnsafe
    //////////////////////////////////////////////////////////////////////////////////////////////////

    private final class NioMessageUnsafe extends AbstractNioUnsafe {

        private final List<Object> readBuf = new ArrayList<Object>();

        @Override
        public void read() {
            assert eventLoop().inEventLoop();
            final ChannelConfig config = config();
            final ChannelPipeline pipeline = pipeline();
            final RecvByteBufAllocator.Handle allocHandle = unsafe().recvBufAllocHandle();
            allocHandle.reset(config);

            boolean closed = false;
            Throwable exception = null;
            try {
                try {
                    do {
                        // 模板方法，NioServerSocketChannel#doReadMessages
                        int localRead = doReadMessages(readBuf);
                        // 没有数据可读
                        if (localRead == 0) {
                            break;
                        }
                        // 读取出错
                        if (localRead < 0) {
                            closed = true;
                            break;
                        }

                        allocHandle.incMessagesRead(localRead);
                    } while (allocHandle.continueReading());
                } catch (Throwable t) {
                    exception = t;
                }

                int size = readBuf.size();
                for (int i = 0; i < size; i++) {
                    readPending = false;
                    // 触发ChannelRead事件
                    pipeline.fireChannelRead(readBuf.get(i));
                }

                readBuf.clear();

                allocHandle.readComplete();
                // 触发ChannelReadComplete事件
                // ChannelReadComplete事件中如果配置autoRead则会调用beginRead，从而不断进行读操作
                pipeline.fireChannelReadComplete();

                if (exception != null) {
                    // ServerChannel异常也不能关闭，应该恢复读取下一个客户端
                    closed = closeOnReadError(exception);

                    pipeline.fireExceptionCaught(exception);
                }

                if (closed) {
                    inputShutdown = true;
                    if (isOpen()) {
                        // 非serverChannel且打开则关闭
                        close(voidPromise());
                    }
                }
            } finally {

                if (!readPending && !config.isAutoRead()) {
                    // 清除read事件，不再关心
                    removeReadOp();
                }
            }
        }
    }
}
