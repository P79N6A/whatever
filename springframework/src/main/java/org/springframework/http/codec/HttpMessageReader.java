package org.springframework.http.codec;

import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public interface HttpMessageReader<T> {

    List<MediaType> getReadableMediaTypes();

    boolean canRead(ResolvableType elementType, @Nullable MediaType mediaType);

    Flux<T> read(ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints);

    Mono<T> readMono(ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints);

    default Flux<T> read(ResolvableType actualType, ResolvableType elementType, ServerHttpRequest request, ServerHttpResponse response, Map<String, Object> hints) {
        return read(elementType, request, hints);
    }

    default Mono<T> readMono(ResolvableType actualType, ResolvableType elementType, ServerHttpRequest request, ServerHttpResponse response, Map<String, Object> hints) {
        return readMono(elementType, request, hints);
    }

}
