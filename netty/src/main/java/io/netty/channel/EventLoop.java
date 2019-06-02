package io.netty.channel;

import io.netty.util.concurrent.OrderedEventExecutor;

/**
 * EventLoop继承了EventLoopGroup
 */
public interface EventLoop extends OrderedEventExecutor, EventLoopGroup {
    @Override
    EventLoopGroup parent();
}
