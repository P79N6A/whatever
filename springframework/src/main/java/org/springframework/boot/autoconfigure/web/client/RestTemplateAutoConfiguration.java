package org.springframework.boot.autoconfigure.web.client;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.condition.NoneNestedConditions;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration.NotReactiveWebApplicationCondition;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(HttpMessageConvertersAutoConfiguration.class)
@ConditionalOnClass(RestTemplate.class)
@Conditional(NotReactiveWebApplicationCondition.class)
public class RestTemplateAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RestTemplateBuilder restTemplateBuilder(ObjectProvider<HttpMessageConverters> messageConverters, ObjectProvider<RestTemplateCustomizer> restTemplateCustomizers) {
        RestTemplateBuilder builder = new RestTemplateBuilder();
        HttpMessageConverters converters = messageConverters.getIfUnique();
        if (converters != null) {
            builder = builder.messageConverters(converters.getConverters());
        }
        List<RestTemplateCustomizer> customizers = restTemplateCustomizers.orderedStream().collect(Collectors.toList());
        if (!customizers.isEmpty()) {
            builder = builder.customizers(customizers);
        }
        return builder;
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
