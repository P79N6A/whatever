package org.springframework.core.codec;

import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

import java.util.Map;

public class ByteArrayEncoder extends AbstractEncoder<byte[]> {

    public ByteArrayEncoder() {
        super(MimeTypeUtils.ALL);
    }

    @Override
    public boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType) {
        Class<?> clazz = elementType.toClass();
        return super.canEncode(elementType, mimeType) && byte[].class.isAssignableFrom(clazz);
    }

    @Override
    public Flux<DataBuffer> encode(Publisher<? extends byte[]> inputStream, DataBufferFactory bufferFactory, ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
        // Use (byte[] bytes) for Eclipse
        return Flux.from(inputStream).map((byte[] bytes) -> encodeValue(bytes, bufferFactory, elementType, mimeType, hints));
    }

    @Override
    public DataBuffer encodeValue(byte[] bytes, DataBufferFactory bufferFactory, ResolvableType valueType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
        DataBuffer dataBuffer = bufferFactory.wrap(bytes);
        if (logger.isDebugEnabled() && !Hints.isLoggingSuppressed(hints)) {
            String logPrefix = Hints.getLogPrefix(hints);
            logger.debug(logPrefix + "Writing " + dataBuffer.readableByteCount() + " bytes");
        }
        return dataBuffer;
    }

}
