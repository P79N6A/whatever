package org.springframework.core.codec;

import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

import java.util.Map;

public class DataBufferDecoder extends AbstractDataBufferDecoder<DataBuffer> {

    public DataBufferDecoder() {
        super(MimeTypeUtils.ALL);
    }

    @Override
    public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
        return (DataBuffer.class.isAssignableFrom(elementType.toClass()) && super.canDecode(elementType, mimeType));
    }

    @Override
    public Flux<DataBuffer> decode(Publisher<DataBuffer> input, ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
        return Flux.from(input);
    }

    @Override
    public DataBuffer decode(DataBuffer buffer, ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
        if (logger.isDebugEnabled()) {
            logger.debug(Hints.getLogPrefix(hints) + "Read " + buffer.readableByteCount() + " bytes");
        }
        return buffer;
    }

}
