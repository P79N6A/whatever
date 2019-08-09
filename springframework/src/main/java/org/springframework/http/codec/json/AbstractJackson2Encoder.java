package org.springframework.http.codec.json;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import org.reactivestreams.Publisher;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CodecException;
import org.springframework.core.codec.EncodingException;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageEncoder;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.util.*;

public abstract class AbstractJackson2Encoder extends Jackson2CodecSupport implements HttpMessageEncoder<Object> {

    private static final byte[] NEWLINE_SEPARATOR = {'\n'};

    private static final Map<MediaType, byte[]> STREAM_SEPARATORS;

    static {
        STREAM_SEPARATORS = new HashMap<>();
        STREAM_SEPARATORS.put(MediaType.APPLICATION_STREAM_JSON, NEWLINE_SEPARATOR);
        STREAM_SEPARATORS.put(MediaType.parseMediaType("application/stream+x-jackson-smile"), new byte[0]);
    }

    private final List<MediaType> streamingMediaTypes = new ArrayList<>(1);

    protected AbstractJackson2Encoder(ObjectMapper mapper, MimeType... mimeTypes) {
        super(mapper, mimeTypes);
    }

    public void setStreamingMediaTypes(List<MediaType> mediaTypes) {
        this.streamingMediaTypes.clear();
        this.streamingMediaTypes.addAll(mediaTypes);
    }

    @Override
    public boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType) {
        Class<?> clazz = elementType.toClass();
        return supportsMimeType(mimeType) && (Object.class == clazz || (!String.class.isAssignableFrom(elementType.resolve(clazz)) && getObjectMapper().canSerialize(clazz)));
    }

    @Override
    public Flux<DataBuffer> encode(Publisher<?> inputStream, DataBufferFactory bufferFactory, ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
        Assert.notNull(inputStream, "'inputStream' must not be null");
        Assert.notNull(bufferFactory, "'bufferFactory' must not be null");
        Assert.notNull(elementType, "'elementType' must not be null");
        JsonEncoding encoding = getJsonEncoding(mimeType);
        if (inputStream instanceof Mono) {
            return Mono.from(inputStream).map(value -> encodeValue(value, bufferFactory, elementType, mimeType, hints, encoding)).flux();
        } else {
            return this.streamingMediaTypes.stream().filter(mediaType -> mediaType.isCompatibleWith(mimeType)).findFirst().map(mediaType -> {
                byte[] separator = STREAM_SEPARATORS.getOrDefault(mediaType, NEWLINE_SEPARATOR);
                return Flux.from(inputStream).map(value -> {
                    DataBuffer buffer = encodeValue(value, bufferFactory, elementType, mimeType, hints, encoding);
                    if (separator != null) {
                        buffer.write(separator);
                    }
                    return buffer;
                });
            }).orElseGet(() -> {
                ResolvableType listType = ResolvableType.forClassWithGenerics(List.class, elementType);
                return Flux.from(inputStream).collectList().map(list -> encodeValue(list, bufferFactory, listType, mimeType, hints, encoding)).flux();
            });
        }
    }

    @Override
    public DataBuffer encodeValue(Object value, DataBufferFactory bufferFactory, ResolvableType valueType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
        return encodeValue(value, bufferFactory, valueType, mimeType, hints, getJsonEncoding(mimeType));
    }

    private DataBuffer encodeValue(Object value, DataBufferFactory bufferFactory, ResolvableType valueType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints, JsonEncoding encoding) {
        if (!Hints.isLoggingSuppressed(hints)) {
            LogFormatUtils.traceDebug(logger, traceOn -> {
                String formatted = LogFormatUtils.formatValue(value, !traceOn);
                return Hints.getLogPrefix(hints) + "Encoding [" + formatted + "]";
            });
        }
        JavaType javaType = getJavaType(valueType.getType(), null);
        Class<?> jsonView = (hints != null ? (Class<?>) hints.get(Jackson2CodecSupport.JSON_VIEW_HINT) : null);
        ObjectWriter writer = (jsonView != null ? getObjectMapper().writerWithView(jsonView) : getObjectMapper().writer());
        if (javaType.isContainerType()) {
            writer = writer.forType(javaType);
        }
        writer = customizeWriter(writer, mimeType, valueType, hints);
        DataBuffer buffer = bufferFactory.allocateBuffer();
        boolean release = true;
        OutputStream outputStream = buffer.asOutputStream();
        try {
            JsonGenerator generator = getObjectMapper().getFactory().createGenerator(outputStream, encoding);
            writer.writeValue(generator, value);
            generator.flush();
            release = false;
        } catch (InvalidDefinitionException ex) {
            throw new CodecException("Type definition error: " + ex.getType(), ex);
        } catch (JsonProcessingException ex) {
            throw new EncodingException("JSON encoding error: " + ex.getOriginalMessage(), ex);
        } catch (IOException ex) {
            throw new IllegalStateException("Unexpected I/O error while writing to data buffer", ex);
        } finally {
            if (release) {
                DataBufferUtils.release(buffer);
            }
        }
        return buffer;
    }

    protected ObjectWriter customizeWriter(ObjectWriter writer, @Nullable MimeType mimeType, ResolvableType elementType, @Nullable Map<String, Object> hints) {
        return writer;
    }

    protected JsonEncoding getJsonEncoding(@Nullable MimeType mimeType) {
        if (mimeType != null && mimeType.getCharset() != null) {
            Charset charset = mimeType.getCharset();
            for (JsonEncoding encoding : JsonEncoding.values()) {
                if (charset.name().equals(encoding.getJavaName())) {
                    return encoding;
                }
            }
        }
        return JsonEncoding.UTF8;
    }
    // HttpMessageEncoder...

    @Override
    public List<MimeType> getEncodableMimeTypes() {
        return getMimeTypes();
    }

    @Override
    public List<MediaType> getStreamingMediaTypes() {
        return Collections.unmodifiableList(this.streamingMediaTypes);
    }

    public Map<String, Object> getEncodeHints(@Nullable ResolvableType actualType, ResolvableType elementType, @Nullable MediaType mediaType, ServerHttpRequest request, ServerHttpResponse response) {
        return (actualType != null ? getHints(actualType) : Hints.none());
    }
    // Jackson2CodecSupport ...

    @Override
    protected <A extends Annotation> A getAnnotation(MethodParameter parameter, Class<A> annotType) {
        return parameter.getMethodAnnotation(annotType);
    }

}
