package org.springframework.http.codec;

import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;

public interface ClientCodecConfigurer extends CodecConfigurer {

    @Override
    ClientDefaultCodecs defaultCodecs();

    static ClientCodecConfigurer create() {
        return CodecConfigurerFactory.create(ClientCodecConfigurer.class);
    }

    interface ClientDefaultCodecs extends DefaultCodecs {

        MultipartCodecs multipartCodecs();

        void serverSentEventDecoder(Decoder<?> decoder);

    }

    interface MultipartCodecs {

        MultipartCodecs encoder(Encoder<?> encoder);

        MultipartCodecs writer(HttpMessageWriter<?> writer);

    }

}
