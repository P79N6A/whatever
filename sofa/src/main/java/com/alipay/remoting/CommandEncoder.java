package com.alipay.remoting;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.io.Serializable;

public interface CommandEncoder {

    void encode(ChannelHandlerContext ctx, Serializable msg, ByteBuf out) throws Exception;

}
