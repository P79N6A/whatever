package org.springframework.boot.autoconfigure.jackson;

import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@FunctionalInterface
public interface Jackson2ObjectMapperBuilderCustomizer {

    void customize(Jackson2ObjectMapperBuilder jacksonObjectMapperBuilder);

}
