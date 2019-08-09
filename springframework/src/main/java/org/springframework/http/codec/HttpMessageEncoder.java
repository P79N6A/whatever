package org.springframework.http.codec;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Encoder;
import org.springframework.core.codec.Hints;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Map;

public interface HttpMessageEncoder<T> extends Encoder<T> {

    List<MediaType> getStreamingMediaTypes();

    default Map<String, Object> getEncodeHints(ResolvableType actualType, ResolvableType elementType, @Nullable MediaType mediaType, ServerHttpRequest request, ServerHttpResponse response) {
        return Hints.none();
    }

}
