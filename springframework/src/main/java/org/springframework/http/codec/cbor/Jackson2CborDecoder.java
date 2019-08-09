package org.springframework.http.codec.cbor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.AbstractJackson2Decoder;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;

import java.util.Map;

public class Jackson2CborDecoder extends AbstractJackson2Decoder {

    public Jackson2CborDecoder() {
        this(Jackson2ObjectMapperBuilder.cbor().build(), new MediaType("application", "cbor"));
    }

    public Jackson2CborDecoder(ObjectMapper mapper, MimeType... mimeTypes) {
        super(mapper, mimeTypes);
        Assert.isAssignable(CBORFactory.class, mapper.getFactory().getClass());
    }

    @Override
    public Flux<Object> decode(Publisher<DataBuffer> input, ResolvableType elementType, MimeType mimeType, Map<String, Object> hints) {
        throw new UnsupportedOperationException("Does not support stream decoding yet");
    }

}
