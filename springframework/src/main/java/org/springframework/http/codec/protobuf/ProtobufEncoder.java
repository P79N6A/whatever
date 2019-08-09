package org.springframework.http.codec.protobuf;

import com.google.protobuf.Message;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageEncoder;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProtobufEncoder extends ProtobufCodecSupport implements HttpMessageEncoder<Message> {

    private static final List<MediaType> streamingMediaTypes = MIME_TYPES.stream().map(mimeType -> new MediaType(mimeType.getType(), mimeType.getSubtype(), Collections.singletonMap(DELIMITED_KEY, DELIMITED_VALUE))).collect(Collectors.toList());

    @Override
    public boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType) {
        return Message.class.isAssignableFrom(elementType.toClass()) && supportsMimeType(mimeType);
    }

    @Override
    public Flux<DataBuffer> encode(Publisher<? extends Message> inputStream, DataBufferFactory bufferFactory, ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
        return Flux.from(inputStream).map(message -> encodeValue(message, bufferFactory, !(inputStream instanceof Mono)));
    }

    @Override
    public DataBuffer encodeValue(Message message, DataBufferFactory bufferFactory, ResolvableType valueType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
        return encodeValue(message, bufferFactory, false);
    }

    private DataBuffer encodeValue(Message message, DataBufferFactory bufferFactory, boolean delimited) {
        DataBuffer buffer = bufferFactory.allocateBuffer();
        boolean release = true;
        try {
            if (delimited) {
                message.writeDelimitedTo(buffer.asOutputStream());
            } else {
                message.writeTo(buffer.asOutputStream());
            }
            release = false;
            return buffer;
        } catch (IOException ex) {
            throw new IllegalStateException("Unexpected I/O error while writing to data buffer", ex);
        } finally {
            if (release) {
                DataBufferUtils.release(buffer);
            }
        }
    }

    @Override
    public List<MediaType> getStreamingMediaTypes() {
        return streamingMediaTypes;
    }

    @Override
    public List<MimeType> getEncodableMimeTypes() {
        return getMimeTypes();
    }

}
