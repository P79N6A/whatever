package org.apache.dubbo.remoting.transport.codec;

import org.apache.dubbo.common.io.UnsafeByteArrayInputStream;
import org.apache.dubbo.common.io.UnsafeByteArrayOutputStream;
import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.Codec;
import org.apache.dubbo.remoting.Codec2;
import org.apache.dubbo.remoting.buffer.ChannelBuffer;

import java.io.IOException;

public class CodecAdapter implements Codec2 {

    private Codec codec;

    public CodecAdapter(Codec codec) {
        Assert.notNull(codec, "codec == null");
        this.codec = codec;
    }

    @Override
    public void encode(Channel channel, ChannelBuffer buffer, Object message) throws IOException {
        UnsafeByteArrayOutputStream os = new UnsafeByteArrayOutputStream(1024);
        codec.encode(channel, os, message);
        buffer.writeBytes(os.toByteArray());
    }

    @Override
    public Object decode(Channel channel, ChannelBuffer buffer) throws IOException {
        byte[] bytes = new byte[buffer.readableBytes()];
        int savedReaderIndex = buffer.readerIndex();
        buffer.readBytes(bytes);
        UnsafeByteArrayInputStream is = new UnsafeByteArrayInputStream(bytes);
        Object result = codec.decode(channel, is);
        buffer.readerIndex(savedReaderIndex + is.position());
        return result == Codec.NEED_MORE_INPUT ? DecodeResult.NEED_MORE_INPUT : result;
    }

    public Codec getCodec() {
        return codec;
    }

}
