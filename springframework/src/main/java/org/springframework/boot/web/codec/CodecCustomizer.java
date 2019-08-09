package org.springframework.boot.web.codec;

import org.springframework.http.codec.CodecConfigurer;

@FunctionalInterface
public interface CodecCustomizer {

    void customize(CodecConfigurer configurer);

}
