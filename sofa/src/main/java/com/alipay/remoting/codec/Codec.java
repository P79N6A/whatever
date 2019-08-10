package com.alipay.remoting.codec;

import io.netty.channel.ChannelHandler;

public interface Codec {

    ChannelHandler newEncoder();

    ChannelHandler newDecoder();

}
