package io.netty.channel;

import java.lang.annotation.*;

/**
 * ChannelHandler不处理事件，由子类处理：ChannelInboundHandler拦截和处理入站事件，ChannelOutboundHandler拦截和处理出站事件
 * ChannelHandler和ChannelHandlerContext通过组合或继承的方式关联到一起
 * 事件通过ChannelHandlerContext主动调用如fireXXX()和write(msg)等方法，将事件传播到下一个处理器
 * 入站事件在ChannelPipeline中由头到尾传播，出站事件则相反
 */
public interface ChannelHandler {

    /**
     * Handler被添加到ChannelPipeline时调用
     */
    void handlerAdded(ChannelHandlerContext ctx) throws Exception;

    /**
     * Handler从ChannelPipeline中删除时调用
     */
    void handlerRemoved(ChannelHandlerContext ctx) throws Exception;

    /**
     * 发生异常时调用
     */
    @Deprecated
    void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception;

    /**
     * 要求状态无关
     */
    @Inherited
    @Documented
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Sharable {

    }
}
