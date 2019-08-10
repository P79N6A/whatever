package com.alipay.remoting;

import io.netty.channel.ChannelHandlerContext;

public interface HeartbeatTrigger {
    void heartbeatTriggered(final ChannelHandlerContext ctx) throws Exception;

}
