package io.netty.handler.traffic;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.util.ArrayDeque;
import java.util.concurrent.TimeUnit;

public class ChannelTrafficShapingHandler extends AbstractTrafficShapingHandler {
    private final ArrayDeque<ToSend> messagesQueue = new ArrayDeque<ToSend>();
    private long queueSize;

    public ChannelTrafficShapingHandler(long writeLimit, long readLimit, long checkInterval, long maxTime) {
        super(writeLimit, readLimit, checkInterval, maxTime);
    }

    public ChannelTrafficShapingHandler(long writeLimit, long readLimit, long checkInterval) {
        super(writeLimit, readLimit, checkInterval);
    }

    public ChannelTrafficShapingHandler(long writeLimit, long readLimit) {
        super(writeLimit, readLimit);
    }

    public ChannelTrafficShapingHandler(long checkInterval) {
        super(checkInterval);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        TrafficCounter trafficCounter = new TrafficCounter(this, ctx.executor(), "ChannelTC" + ctx.channel().hashCode(), checkInterval);
        setTrafficCounter(trafficCounter);
        trafficCounter.start();
        super.handlerAdded(ctx);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        trafficCounter.stop();

        synchronized (this) {
            if (ctx.channel().isActive()) {
                for (ToSend toSend : messagesQueue) {
                    long size = calculateSize(toSend.toSend);
                    trafficCounter.bytesRealWriteFlowControl(size);
                    queueSize -= size;
                    ctx.write(toSend.toSend, toSend.promise);
                }
            } else {
                for (ToSend toSend : messagesQueue) {
                    if (toSend.toSend instanceof ByteBuf) {
                        ((ByteBuf) toSend.toSend).release();
                    }
                }
            }
            messagesQueue.clear();
        }
        releaseWriteSuspended(ctx);
        releaseReadSuspended(ctx);
        super.handlerRemoved(ctx);
    }

    private static final class ToSend {
        final long relativeTimeAction;
        final Object toSend;
        final ChannelPromise promise;

        private ToSend(final long delay, final Object toSend, final ChannelPromise promise) {
            relativeTimeAction = delay;
            this.toSend = toSend;
            this.promise = promise;
        }
    }

    @Override
    void submitWrite(final ChannelHandlerContext ctx, final Object msg, final long size, final long delay, final long now, final ChannelPromise promise) {
        final ToSend newToSend;

        synchronized (this) {
            if (delay == 0 && messagesQueue.isEmpty()) {
                trafficCounter.bytesRealWriteFlowControl(size);
                ctx.write(msg, promise);
                return;
            }
            newToSend = new ToSend(delay + now, msg, promise);
            messagesQueue.addLast(newToSend);
            queueSize += size;
            checkWriteSuspend(ctx, delay, queueSize);
        }
        final long futureNow = newToSend.relativeTimeAction;
        ctx.executor().schedule(new Runnable() {
            @Override
            public void run() {
                sendAllValid(ctx, futureNow);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private void sendAllValid(final ChannelHandlerContext ctx, final long now) {

        synchronized (this) {
            ToSend newToSend = messagesQueue.pollFirst();
            for (; newToSend != null; newToSend = messagesQueue.pollFirst()) {
                if (newToSend.relativeTimeAction <= now) {
                    long size = calculateSize(newToSend.toSend);
                    trafficCounter.bytesRealWriteFlowControl(size);
                    queueSize -= size;
                    ctx.write(newToSend.toSend, newToSend.promise);
                } else {
                    messagesQueue.addFirst(newToSend);
                    break;
                }
            }
            if (messagesQueue.isEmpty()) {
                releaseWriteSuspended(ctx);
            }
        }
        ctx.flush();
    }

    public long queueSize() {
        return queueSize;
    }
}
