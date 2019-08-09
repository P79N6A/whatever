package org.springframework.boot.autoconfigure.http;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.condition.NoneNestedConditions;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration.NotReactiveWebApplicationCondition;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jsonb.JsonbAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;

import java.util.stream.Collectors;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(HttpMessageConverter.class)
@Conditional(NotReactiveWebApplicationCondition.class)
@AutoConfigureAfter({GsonAutoConfiguration.class, JacksonAutoConfiguration.class, JsonbAutoConfiguration.class})
@Import({JacksonHttpMessageConvertersConfiguration.class, GsonHttpMessageConvertersConfiguration.class, JsonbHttpMessageConvertersConfiguration.class})
public class HttpMessageConvertersAutoConfiguration {

    static final String PREFERRED_MAPPER_PROPERTY = "spring.http.converters.preferred-json-mapper";

    @Bean
    @ConditionalOnMissingBean
    public HttpMessageConverters messageConverters(ObjectProvider<HttpMessageConverter<?>> converters) {
        return new HttpMessageConverters(converters.orderedStream().collect(Collectors.toList()));
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(StringHttpMessageConverter.class)
    @EnableConfigurationProperties(HttpProperties.class)
    protected static class StringHttpMessageConverterConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public StringHttpMessageConverter stringHttpMessageConverter(HttpProperties httpProperties) {
            StringHttpMessageConverter converter = new StringHttpMessageConverter(httpProperties.getEncoding().getCharset());
            converter.setWriteAcceptCharset(false);
            return converter;
        }

    }

    static class NotReactiveWebApplicationCondition extends NoneNestedConditions {

        NotReactiveWebApplicationCondition() {
            super(ConfigurationPhase.PARSE_CONFIGURATION);
        }

        @ConditionalOnWebApplication(type = Type.REACTIVE)
        private static class ReactiveWebApplication {

        }

    }

}
