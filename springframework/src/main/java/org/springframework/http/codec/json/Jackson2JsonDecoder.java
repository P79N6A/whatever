package org.springframework.http.codec.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.MimeType;

public class Jackson2JsonDecoder extends AbstractJackson2Decoder {

    public Jackson2JsonDecoder() {
        super(Jackson2ObjectMapperBuilder.json().build());
    }

    public Jackson2JsonDecoder(ObjectMapper mapper, MimeType... mimeTypes) {
        super(mapper, mimeTypes);
    }

}
