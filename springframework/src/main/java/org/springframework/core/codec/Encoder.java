package org.springframework.core.codec;

import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

public interface Encoder<T> {

    boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType);

    Flux<DataBuffer> encode(Publisher<? extends T> inputStream, DataBufferFactory bufferFactory, ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints);

    default DataBuffer encodeValue(T value, DataBufferFactory bufferFactory, ResolvableType valueType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
        // It may not be possible to produce a single DataBuffer synchronously
        throw new UnsupportedOperationException();
    }

    List<MimeType> getEncodableMimeTypes();

}
