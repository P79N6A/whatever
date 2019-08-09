package org.springframework.http.codec;

import org.springframework.core.codec.Encoder;
import org.springframework.http.codec.multipart.Part;

public interface ServerCodecConfigurer extends CodecConfigurer {

    @Override
    ServerDefaultCodecs defaultCodecs();

    static ServerCodecConfigurer create() {
        return CodecConfigurerFactory.create(ServerCodecConfigurer.class);
    }

    interface ServerDefaultCodecs extends DefaultCodecs {

        void serverSentEventEncoder(Encoder<?> encoder);

        void multipartReader(HttpMessageReader<Part> multipartReader);

    }

}
