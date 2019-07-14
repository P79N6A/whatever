package org.apache.dubbo.remoting;

import org.apache.dubbo.common.constants.RemotingConstants;
import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.SPI;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Deprecated
@SPI
public interface Codec {

    Object NEED_MORE_INPUT = new Object();

    @Adaptive({RemotingConstants.CODEC_KEY})
    void encode(Channel channel, OutputStream output, Object message) throws IOException;

    @Adaptive({RemotingConstants.CODEC_KEY})
    Object decode(Channel channel, InputStream input) throws IOException;

}