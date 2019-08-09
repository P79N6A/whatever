package org.springframework.core.codec;

import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;

import java.util.Map;

public abstract class AbstractSingleValueEncoder<T> extends AbstractEncoder<T> {

    public AbstractSingleValueEncoder(MimeType... supportedMimeTypes) {
        super(supportedMimeTypes);
    }

    @Override
    public final Flux<DataBuffer> encode(Publisher<? extends T> inputStream, DataBufferFactory bufferFactory, ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
        return Flux.from(inputStream).take(1).concatMap(value -> encode(value, bufferFactory, elementType, mimeType, hints)).doOnDiscard(PooledDataBuffer.class, PooledDataBuffer::release);
    }

    protected abstract Flux<DataBuffer> encode(T t, DataBufferFactory dataBufferFactory, ResolvableType type, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints);

}
