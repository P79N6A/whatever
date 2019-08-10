package com.alipay.remoting;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;

public interface CommandDecoder {

    void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception;

}
