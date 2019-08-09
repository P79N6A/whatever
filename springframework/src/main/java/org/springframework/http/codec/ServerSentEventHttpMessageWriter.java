package org.springframework.http.codec;

import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CodecException;
import org.springframework.core.codec.Encoder;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ServerSentEventHttpMessageWriter implements HttpMessageWriter<Object> {

    private static final MediaType DEFAULT_MEDIA_TYPE = new MediaType("text", "event-stream", StandardCharsets.UTF_8);

    private static final List<MediaType> WRITABLE_MEDIA_TYPES = Collections.singletonList(MediaType.TEXT_EVENT_STREAM);

    @Nullable
    private final Encoder<?> encoder;

    public ServerSentEventHttpMessageWriter() {
        this(null);
    }

    public ServerSentEventHttpMessageWriter(@Nullable Encoder<?> encoder) {
        this.encoder = encoder;
    }

    @Nullable
    public Encoder<?> getEncoder() {
        return this.encoder;
    }

    @Override
    public List<MediaType> getWritableMediaTypes() {
        return WRITABLE_MEDIA_TYPES;
    }

    @Override
    public boolean canWrite(ResolvableType elementType, @Nullable MediaType mediaType) {
        return (mediaType == null || MediaType.TEXT_EVENT_STREAM.includes(mediaType) || ServerSentEvent.class.isAssignableFrom(elementType.toClass()));
    }

    @Override
    public Mono<Void> write(Publisher<?> input, ResolvableType elementType, @Nullable MediaType mediaType, ReactiveHttpOutputMessage message, Map<String, Object> hints) {
        mediaType = (mediaType != null && mediaType.getCharset() != null ? mediaType : DEFAULT_MEDIA_TYPE);
        DataBufferFactory bufferFactory = message.bufferFactory();
        message.getHeaders().setContentType(mediaType);
        return message.writeAndFlushWith(encode(input, elementType, mediaType, bufferFactory, hints));
    }

    private Flux<Publisher<DataBuffer>> encode(Publisher<?> input, ResolvableType elementType, MediaType mediaType, DataBufferFactory bufferFactory, Map<String, Object> hints) {
        ResolvableType dataType = (ServerSentEvent.class.isAssignableFrom(elementType.toClass()) ? elementType.getGeneric() : elementType);
        return Flux.from(input).map(element -> {
            ServerSentEvent<?> sse = (element instanceof ServerSentEvent ? (ServerSentEvent<?>) element : ServerSentEvent.builder().data(element).build());
            StringBuilder sb = new StringBuilder();
            String id = sse.id();
            String event = sse.event();
            Duration retry = sse.retry();
            String comment = sse.comment();
            Object data = sse.data();
            if (id != null) {
                writeField("id", id, sb);
            }
            if (event != null) {
                writeField("event", event, sb);
            }
            if (retry != null) {
                writeField("retry", retry.toMillis(), sb);
            }
            if (comment != null) {
                sb.append(':').append(StringUtils.replace(comment, "\n", "\n:")).append("\n");
            }
            if (data != null) {
                sb.append("data:");
            }
            Mono<DataBuffer> bufferMono = Mono.fromCallable(() -> bufferFactory.join(encodeEvent(sb, data, dataType, mediaType, bufferFactory, hints)));
            return bufferMono.doOnDiscard(PooledDataBuffer.class, DataBufferUtils::release);
        });
    }

    private void writeField(String fieldName, Object fieldValue, StringBuilder sb) {
        sb.append(fieldName);
        sb.append(':');
        sb.append(fieldValue.toString());
        sb.append("\n");
    }

    @SuppressWarnings("unchecked")
    private <T> List<DataBuffer> encodeEvent(CharSequence markup, @Nullable T data, ResolvableType dataType, MediaType mediaType, DataBufferFactory factory, Map<String, Object> hints) {
        List<DataBuffer> result = new ArrayList<>(4);
        result.add(encodeText(markup, mediaType, factory));
        if (data != null) {
            if (data instanceof String) {
                String dataLine = StringUtils.replace((String) data, "\n", "\ndata:") + "\n";
                result.add(encodeText(dataLine, mediaType, factory));
            } else if (this.encoder == null) {
                throw new CodecException("No SSE encoder configured and the data is not String.");
            } else {
                result.add(((Encoder<T>) this.encoder).encodeValue(data, factory, dataType, mediaType, hints));
                result.add(encodeText("\n", mediaType, factory));
            }
        }
        result.add(encodeText("\n", mediaType, factory));
        return result;
    }

    private DataBuffer encodeText(CharSequence text, MediaType mediaType, DataBufferFactory bufferFactory) {
        Assert.notNull(mediaType.getCharset(), "Expected MediaType with charset");
        byte[] bytes = text.toString().getBytes(mediaType.getCharset());
        return bufferFactory.wrap(bytes); // wrapping, not allocating
    }

    public Mono<Void> write(Publisher<?> input, ResolvableType actualType, ResolvableType elementType, @Nullable MediaType mediaType, ServerHttpRequest request, ServerHttpResponse response, Map<String, Object> hints) {
        Map<String, Object> allHints = Hints.merge(hints, getEncodeHints(actualType, elementType, mediaType, request, response));
        return write(input, elementType, mediaType, response, allHints);
    }

    private Map<String, Object> getEncodeHints(ResolvableType actualType, ResolvableType elementType, @Nullable MediaType mediaType, ServerHttpRequest request, ServerHttpResponse response) {
        if (this.encoder instanceof HttpMessageEncoder) {
            HttpMessageEncoder<?> encoder = (HttpMessageEncoder<?>) this.encoder;
            return encoder.getEncodeHints(actualType, elementType, mediaType, request, response);
        }
        return Hints.none();
    }

}
