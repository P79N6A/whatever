package org.springframework.http.codec.protobuf;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.codec.Encoder;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.HttpMessageEncoder;
import org.springframework.lang.Nullable;
import org.springframework.util.ConcurrentReferenceHashMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

public class ProtobufHttpMessageWriter extends EncoderHttpMessageWriter<Message> {

    private static final String X_PROTOBUF_SCHEMA_HEADER = "X-Protobuf-Schema";

    private static final String X_PROTOBUF_MESSAGE_HEADER = "X-Protobuf-Message";

    private static final ConcurrentMap<Class<?>, Method> methodCache = new ConcurrentReferenceHashMap<>();

    public ProtobufHttpMessageWriter() {
        super(new ProtobufEncoder());
    }

    public ProtobufHttpMessageWriter(Encoder<Message> encoder) {
        super(encoder);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Mono<Void> write(Publisher<? extends Message> inputStream, ResolvableType elementType, @Nullable MediaType mediaType, ReactiveHttpOutputMessage message, Map<String, Object> hints) {
        try {
            Message.Builder builder = getMessageBuilder(elementType.toClass());
            Descriptors.Descriptor descriptor = builder.getDescriptorForType();
            message.getHeaders().add(X_PROTOBUF_SCHEMA_HEADER, descriptor.getFile().getName());
            message.getHeaders().add(X_PROTOBUF_MESSAGE_HEADER, descriptor.getFullName());
            if (inputStream instanceof Flux) {
                if (mediaType == null) {
                    message.getHeaders().setContentType(((HttpMessageEncoder<?>) getEncoder()).getStreamingMediaTypes().get(0));
                } else if (!ProtobufEncoder.DELIMITED_VALUE.equals(mediaType.getParameters().get(ProtobufEncoder.DELIMITED_KEY))) {
                    Map<String, String> parameters = new HashMap<>(mediaType.getParameters());
                    parameters.put(ProtobufEncoder.DELIMITED_KEY, ProtobufEncoder.DELIMITED_VALUE);
                    message.getHeaders().setContentType(new MediaType(mediaType.getType(), mediaType.getSubtype(), parameters));
                }
            }
            return super.write(inputStream, elementType, mediaType, message, hints);
        } catch (Exception ex) {
            return Mono.error(new DecodingException("Could not read Protobuf message: " + ex.getMessage(), ex));
        }
    }

    private static Message.Builder getMessageBuilder(Class<?> clazz) throws Exception {
        Method method = methodCache.get(clazz);
        if (method == null) {
            method = clazz.getMethod("newBuilder");
            methodCache.put(clazz, method);
        }
        return (Message.Builder) method.invoke(clazz);
    }

}
