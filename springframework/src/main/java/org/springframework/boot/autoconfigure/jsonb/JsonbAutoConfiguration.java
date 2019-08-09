package org.springframework.boot.autoconfigure.jsonb;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Jsonb.class)
@ConditionalOnResource(resources = {"classpath:META-INF/services/javax.json.bind.spi.JsonbProvider", "classpath:META-INF/services/javax.json.spi.JsonProvider"})
public class JsonbAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Jsonb jsonb() {
        return JsonbBuilder.create();
    }

}
