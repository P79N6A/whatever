package org.springframework.core.codec;

import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

import java.util.List;
import java.util.Map;

public interface Decoder<T> {

    boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType);

    Flux<T> decode(Publisher<DataBuffer> inputStream, ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints);

    Mono<T> decodeToMono(Publisher<DataBuffer> inputStream, ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints);

    @SuppressWarnings("ConstantConditions")
    default T decode(DataBuffer buffer, ResolvableType targetType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) throws DecodingException {
        MonoProcessor<T> processor = MonoProcessor.create();
        decodeToMono(Mono.just(buffer), targetType, mimeType, hints).subscribeWith(processor);
        Assert.state(processor.isTerminated(), "DataBuffer decoding should have completed.");
        Throwable ex = processor.getError();
        if (ex != null) {
            throw (ex instanceof CodecException ? (CodecException) ex : new DecodingException("Failed to decode: " + ex.getMessage(), ex));
        }
        return processor.peek();
    }

    List<MimeType> getDecodableMimeTypes();

}
