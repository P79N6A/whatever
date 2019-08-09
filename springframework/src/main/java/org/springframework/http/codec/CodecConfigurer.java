package org.springframework.http.codec;

import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;

import java.util.List;

public interface CodecConfigurer {

    DefaultCodecs defaultCodecs();

    CustomCodecs customCodecs();

    void registerDefaults(boolean registerDefaults);

    List<HttpMessageReader<?>> getReaders();

    List<HttpMessageWriter<?>> getWriters();

    interface DefaultCodecs {

        void jackson2JsonDecoder(Decoder<?> decoder);

        void jackson2JsonEncoder(Encoder<?> encoder);

        void protobufDecoder(Decoder<?> decoder);

        void protobufEncoder(Encoder<?> encoder);

        void jaxb2Decoder(Decoder<?> decoder);

        void jaxb2Encoder(Encoder<?> encoder);

        void enableLoggingRequestDetails(boolean enable);

    }

    interface CustomCodecs {

        void decoder(Decoder<?> decoder);

        void encoder(Encoder<?> encoder);

        void reader(HttpMessageReader<?> reader);

        void writer(HttpMessageWriter<?> writer);

    }

}
