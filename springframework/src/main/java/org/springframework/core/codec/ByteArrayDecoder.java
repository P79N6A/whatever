package org.springframework.core.codec;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.util.Map;

public class ByteArrayDecoder extends AbstractDataBufferDecoder<byte[]> {

    public ByteArrayDecoder() {
        super(MimeTypeUtils.ALL);
    }

    @Override
    public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
        return (elementType.resolve() == byte[].class && super.canDecode(elementType, mimeType));
    }

    @Override
    public byte[] decode(DataBuffer dataBuffer, ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
        byte[] result = new byte[dataBuffer.readableByteCount()];
        dataBuffer.read(result);
        DataBufferUtils.release(dataBuffer);
        if (logger.isDebugEnabled()) {
            logger.debug(Hints.getLogPrefix(hints) + "Read " + result.length + " bytes");
        }
        return result;
    }

}
