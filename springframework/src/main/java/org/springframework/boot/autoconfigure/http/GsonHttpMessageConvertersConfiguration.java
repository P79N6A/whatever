package org.springframework.boot.autoconfigure.http;

import com.google.gson.Gson;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Gson.class)
class GsonHttpMessageConvertersConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(Gson.class)
    @Conditional(PreferGsonOrJacksonAndJsonbUnavailableCondition.class)
    protected static class GsonHttpMessageConverterConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public GsonHttpMessageConverter gsonHttpMessageConverter(Gson gson) {
            GsonHttpMessageConverter converter = new GsonHttpMessageConverter();
            converter.setGson(gson);
            return converter;
        }

    }

    private static class PreferGsonOrJacksonAndJsonbUnavailableCondition extends AnyNestedCondition {

        PreferGsonOrJacksonAndJsonbUnavailableCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnProperty(name = HttpMessageConvertersAutoConfiguration.PREFERRED_MAPPER_PROPERTY, havingValue = "gson")
        static class GsonPreferred {

        }

        @Conditional(JacksonAndJsonbUnavailableCondition.class)
        static class JacksonJsonbUnavailable {

        }

    }

    private static class JacksonAndJsonbUnavailableCondition extends NoneNestedConditions {

        JacksonAndJsonbUnavailableCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnBean(MappingJackson2HttpMessageConverter.class)
        static class JacksonAvailable {

        }

        @ConditionalOnProperty(name = HttpMessageConvertersAutoConfiguration.PREFERRED_MAPPER_PROPERTY, havingValue = "jsonb")
        static class JsonbPreferred {

        }

    }

}
