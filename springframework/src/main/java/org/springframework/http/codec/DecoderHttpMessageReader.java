package org.springframework.http.codec;

import org.apache.commons.logging.Log;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.AbstractDecoder;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Hints;
import org.springframework.http.HttpLogging;
import org.springframework.http.HttpMessage;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public class DecoderHttpMessageReader<T> implements HttpMessageReader<T> {

    private final Decoder<T> decoder;

    private final List<MediaType> mediaTypes;

    public DecoderHttpMessageReader(Decoder<T> decoder) {
        Assert.notNull(decoder, "Decoder is required");
        initLogger(decoder);
        this.decoder = decoder;
        this.mediaTypes = MediaType.asMediaTypes(decoder.getDecodableMimeTypes());
    }

    private static void initLogger(Decoder<?> decoder) {
        if (decoder instanceof AbstractDecoder && decoder.getClass().getName().startsWith("org.springframework.core.codec")) {
            Log logger = HttpLogging.forLog(((AbstractDecoder<?>) decoder).getLogger());
            ((AbstractDecoder<?>) decoder).setLogger(logger);
        }
    }

    public Decoder<T> getDecoder() {
        return this.decoder;
    }

    @Override
    public List<MediaType> getReadableMediaTypes() {
        return this.mediaTypes;
    }

    @Override
    public boolean canRead(ResolvableType elementType, @Nullable MediaType mediaType) {
        return this.decoder.canDecode(elementType, mediaType);
    }

    @Override
    public Flux<T> read(ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints) {
        MediaType contentType = getContentType(message);
        return this.decoder.decode(message.getBody(), elementType, contentType, hints);
    }

    @Override
    public Mono<T> readMono(ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints) {
        MediaType contentType = getContentType(message);
        return this.decoder.decodeToMono(message.getBody(), elementType, contentType, hints);
    }

    @Nullable
    protected MediaType getContentType(HttpMessage inputMessage) {
        MediaType contentType = inputMessage.getHeaders().getContentType();
        return (contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM);
    }
    // Server-side only...

    public Flux<T> read(ResolvableType actualType, ResolvableType elementType, ServerHttpRequest request, ServerHttpResponse response, Map<String, Object> hints) {
        Map<String, Object> allHints = Hints.merge(hints, getReadHints(actualType, elementType, request, response));
        return read(elementType, request, allHints);
    }

    public Mono<T> readMono(ResolvableType actualType, ResolvableType elementType, ServerHttpRequest request, ServerHttpResponse response, Map<String, Object> hints) {
        Map<String, Object> allHints = Hints.merge(hints, getReadHints(actualType, elementType, request, response));
        return readMono(elementType, request, allHints);
    }

    protected Map<String, Object> getReadHints(ResolvableType actualType, ResolvableType elementType, ServerHttpRequest request, ServerHttpResponse response) {
        if (this.decoder instanceof HttpMessageDecoder) {
            HttpMessageDecoder<?> decoder = (HttpMessageDecoder<?>) this.decoder;
            return decoder.getDecodeHints(actualType, elementType, request, response);
        }
        return Hints.none();
    }

}
