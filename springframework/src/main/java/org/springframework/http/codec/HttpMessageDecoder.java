package org.springframework.http.codec;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

import java.util.Map;

public interface HttpMessageDecoder<T> extends Decoder<T> {

    Map<String, Object> getDecodeHints(ResolvableType actualType, ResolvableType elementType, ServerHttpRequest request, ServerHttpResponse response);

}
