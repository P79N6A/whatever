package io.netty.channel;

import io.netty.util.concurrent.EventExecutor;
import io.netty.util.internal.PlatformDependent;

final class FailedChannelFuture extends CompleteChannelFuture {

    private final Throwable cause;

    FailedChannelFuture(Channel channel, EventExecutor executor, Throwable cause) {
        super(channel, executor);
        if (cause == null) {
            throw new NullPointerException("cause");
        }
        this.cause = cause;
    }

    @Override
    public Throwable cause() {
        return cause;
    }

    @Override
    public boolean isSuccess() {
        return false;
    }

    @Override
    public ChannelFuture sync() {
        PlatformDependent.throwException(cause);
        return this;
    }

    @Override
    public ChannelFuture syncUninterruptibly() {
        PlatformDependent.throwException(cause);
        return this;
    }
}
