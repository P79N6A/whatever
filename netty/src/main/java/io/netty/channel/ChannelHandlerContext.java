package io.netty.channel;

import io.netty.buffer.ByteBufAllocator;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.AttributeMap;
import io.netty.util.concurrent.EventExecutor;

/**
 * Context指上下文，ChannelHandler的Context指的是ChannelHandler之间的关系以及ChannelHandler与ChannelPipeline之间的关系
 * 一个Channel对应一个ChannelPipeline
 * 一个ChannelHandlerContext对应一个ChannelHandler
 * 但一个ChannelHandler可以对应多个ChannelHandlerContext
 * 当一个ChannelHandler使用Sharable注解修饰且添加同一个实例对象到不用的Channel时，只有一个ChannelHandler实例对象
 * 但每个Channel中都有一个ChannelHandlerContext对象实例与之对应
 */
public interface ChannelHandlerContext extends AttributeMap, ChannelInboundInvoker, ChannelOutboundInvoker {

    Channel channel();

    EventExecutor executor();

    String name();

    ChannelHandler handler();

    boolean isRemoved();

    @Override
    ChannelHandlerContext fireChannelRegistered();

    @Override
    ChannelHandlerContext fireChannelUnregistered();

    @Override
    ChannelHandlerContext fireChannelActive();

    @Override
    ChannelHandlerContext fireChannelInactive();

    @Override
    ChannelHandlerContext fireExceptionCaught(Throwable cause);

    @Override
    ChannelHandlerContext fireUserEventTriggered(Object evt);

    @Override
    ChannelHandlerContext fireChannelRead(Object msg);

    @Override
    ChannelHandlerContext fireChannelReadComplete();

    @Override
    ChannelHandlerContext fireChannelWritabilityChanged();

    @Override
    ChannelHandlerContext read();

    @Override
    ChannelHandlerContext flush();

    ChannelPipeline pipeline();

    ByteBufAllocator alloc();

    @Deprecated
    @Override
    <T> Attribute<T> attr(AttributeKey<T> key);

    @Deprecated
    @Override
    <T> boolean hasAttr(AttributeKey<T> key);
}
