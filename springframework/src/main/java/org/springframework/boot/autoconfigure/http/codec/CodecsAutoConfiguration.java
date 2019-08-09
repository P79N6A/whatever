package org.springframework.boot.autoconfigure.http.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.http.HttpProperties;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.CodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.util.MimeType;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(CodecConfigurer.class)
@AutoConfigureAfter(JacksonAutoConfiguration.class)
public class CodecsAutoConfiguration {

    private static final MimeType[] EMPTY_MIME_TYPES = {};

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(ObjectMapper.class)
    static class JacksonCodecConfiguration {

        @Bean
        @Order(0)
        @ConditionalOnBean(ObjectMapper.class)
        public CodecCustomizer jacksonCodecCustomizer(ObjectMapper objectMapper) {
            return (configurer) -> {
                CodecConfigurer.DefaultCodecs defaults = configurer.defaultCodecs();
                defaults.jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper, EMPTY_MIME_TYPES));
                defaults.jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper, EMPTY_MIME_TYPES));
            };
        }

    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(HttpProperties.class)
    static class LoggingCodecConfiguration {

        @Bean
        @Order(0)
        public CodecCustomizer loggingCodecCustomizer(HttpProperties properties) {
            return (configurer) -> configurer.defaultCodecs().enableLoggingRequestDetails(properties.isLogRequestDetails());
        }

    }

}
