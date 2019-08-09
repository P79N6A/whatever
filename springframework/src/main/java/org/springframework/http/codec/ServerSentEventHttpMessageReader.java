package org.springframework.http.codec;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CodecException;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ServerSentEventHttpMessageReader implements HttpMessageReader<Object> {

    private static final DataBufferFactory bufferFactory = new DefaultDataBufferFactory();

    private static final StringDecoder stringDecoder = StringDecoder.textPlainOnly();

    private static final ResolvableType STRING_TYPE = ResolvableType.forClass(String.class);

    @Nullable
    private final Decoder<?> decoder;

    public ServerSentEventHttpMessageReader() {
        this(null);
    }

    public ServerSentEventHttpMessageReader(@Nullable Decoder<?> decoder) {
        this.decoder = decoder;
    }

    @Nullable
    public Decoder<?> getDecoder() {
        return this.decoder;
    }

    @Override
    public List<MediaType> getReadableMediaTypes() {
        return Collections.singletonList(MediaType.TEXT_EVENT_STREAM);
    }

    @Override
    public boolean canRead(ResolvableType elementType, @Nullable MediaType mediaType) {
        return (MediaType.TEXT_EVENT_STREAM.includes(mediaType) || isServerSentEvent(elementType));
    }

    private boolean isServerSentEvent(ResolvableType elementType) {
        return ServerSentEvent.class.isAssignableFrom(elementType.toClass());
    }

    @Override
    public Flux<Object> read(ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints) {
        boolean shouldWrap = isServerSentEvent(elementType);
        ResolvableType valueType = (shouldWrap ? elementType.getGeneric() : elementType);
        return stringDecoder.decode(message.getBody(), STRING_TYPE, null, hints).bufferUntil(line -> line.equals("")).concatMap(lines -> Mono.justOrEmpty(buildEvent(lines, valueType, shouldWrap, hints)));
    }

    @Nullable
    private Object buildEvent(List<String> lines, ResolvableType valueType, boolean shouldWrap, Map<String, Object> hints) {
        ServerSentEvent.Builder<Object> sseBuilder = shouldWrap ? ServerSentEvent.builder() : null;
        StringBuilder data = null;
        StringBuilder comment = null;
        for (String line : lines) {
            if (line.startsWith("data:")) {
                data = (data != null ? data : new StringBuilder());
                data.append(line.substring(5).trim()).append("\n");
            }
            if (shouldWrap) {
                if (line.startsWith("id:")) {
                    sseBuilder.id(line.substring(3).trim());
                } else if (line.startsWith("event:")) {
                    sseBuilder.event(line.substring(6).trim());
                } else if (line.startsWith("retry:")) {
                    sseBuilder.retry(Duration.ofMillis(Long.valueOf(line.substring(6).trim())));
                } else if (line.startsWith(":")) {
                    comment = (comment != null ? comment : new StringBuilder());
                    comment.append(line.substring(1).trim()).append("\n");
                }
            }
        }
        Object decodedData = data != null ? decodeData(data.toString(), valueType, hints) : null;
        if (shouldWrap) {
            if (comment != null) {
                sseBuilder.comment(comment.toString().substring(0, comment.length() - 1));
            }
            if (decodedData != null) {
                sseBuilder.data(decodedData);
            }
            return sseBuilder.build();
        } else {
            return decodedData;
        }
    }

    private Object decodeData(String data, ResolvableType dataType, Map<String, Object> hints) {
        if (String.class == dataType.resolve()) {
            return data.substring(0, data.length() - 1);
        }
        if (this.decoder == null) {
            throw new CodecException("No SSE decoder configured and the data is not String.");
        }
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = bufferFactory.wrap(bytes);  // wrapping only, no allocation
        return this.decoder.decode(buffer, dataType, MediaType.TEXT_EVENT_STREAM, hints);
    }

    @Override
    public Mono<Object> readMono(ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints) {
        // We're ahead of String + "*/*"
        // Let's see if we can aggregate the output (lest we time out)...
        if (elementType.resolve() == String.class) {
            Flux<DataBuffer> body = message.getBody();
            return stringDecoder.decodeToMono(body, elementType, null, null).cast(Object.class);
        }
        return Mono.error(new UnsupportedOperationException("ServerSentEventHttpMessageReader only supports reading stream of events as a Flux"));
    }

}
