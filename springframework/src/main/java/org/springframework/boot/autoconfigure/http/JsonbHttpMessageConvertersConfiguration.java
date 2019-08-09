package org.springframework.boot.autoconfigure.http;

import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.json.JsonbHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import javax.json.bind.Jsonb;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Jsonb.class)
class JsonbHttpMessageConvertersConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(Jsonb.class)
    @Conditional(PreferJsonbOrMissingJacksonAndGsonCondition.class)
    protected static class JsonbHttpMessageConverterConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public JsonbHttpMessageConverter jsonbHttpMessageConverter(Jsonb jsonb) {
            JsonbHttpMessageConverter converter = new JsonbHttpMessageConverter();
            converter.setJsonb(jsonb);
            return converter;
        }

    }

    private static class PreferJsonbOrMissingJacksonAndGsonCondition extends AnyNestedCondition {

        PreferJsonbOrMissingJacksonAndGsonCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnProperty(name = HttpMessageConvertersAutoConfiguration.PREFERRED_MAPPER_PROPERTY, havingValue = "jsonb")
        static class JsonbPreferred {

        }

        @ConditionalOnMissingBean({MappingJackson2HttpMessageConverter.class, GsonHttpMessageConverter.class})
        static class JacksonAndGsonMissing {

        }

    }

}
