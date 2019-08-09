package org.springframework.core.codec;

import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@SuppressWarnings("deprecation")
public abstract class AbstractDataBufferDecoder<T> extends AbstractDecoder<T> {

    protected AbstractDataBufferDecoder(MimeType... supportedMimeTypes) {
        super(supportedMimeTypes);
    }

    @Override
    public Flux<T> decode(Publisher<DataBuffer> input, ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
        return Flux.from(input).map(buffer -> decodeDataBuffer(buffer, elementType, mimeType, hints));
    }

    @Override
    public Mono<T> decodeToMono(Publisher<DataBuffer> input, ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
        return DataBufferUtils.join(input).map(buffer -> decodeDataBuffer(buffer, elementType, mimeType, hints));
    }

    @Deprecated
    protected T decodeDataBuffer(DataBuffer buffer, ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
        return decode(buffer, elementType, mimeType, hints);
    }

}
