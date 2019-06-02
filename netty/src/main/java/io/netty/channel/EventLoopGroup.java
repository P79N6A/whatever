package io.netty.channel;

import io.netty.util.concurrent.EventExecutorGroup;

public interface EventLoopGroup extends EventExecutorGroup {

    @Override
    EventLoop next();

    ChannelFuture register(Channel channel);

    ChannelFuture register(ChannelPromise promise);

    @Deprecated
    ChannelFuture register(Channel channel, ChannelPromise promise);
}
