package org.springframework.boot.autoconfigure.context;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties
public class ConfigurationPropertiesAutoConfiguration {

}
