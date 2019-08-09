package org.springframework.http.codec;

import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public interface HttpMessageWriter<T> {

    List<MediaType> getWritableMediaTypes();

    boolean canWrite(ResolvableType elementType, @Nullable MediaType mediaType);

    Mono<Void> write(Publisher<? extends T> inputStream, ResolvableType elementType, @Nullable MediaType mediaType, ReactiveHttpOutputMessage message, Map<String, Object> hints);

    default Mono<Void> write(Publisher<? extends T> inputStream, ResolvableType actualType, ResolvableType elementType, @Nullable MediaType mediaType, ServerHttpRequest request, ServerHttpResponse response, Map<String, Object> hints) {
        return write(inputStream, elementType, mediaType, response, hints);
    }

}
