package org.springframework.boot.autoconfigure.groovy.template;

import groovy.text.markup.MarkupTemplateEngine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.template.TemplateLocation;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.servlet.view.UrlBasedViewResolver;
import org.springframework.web.servlet.view.groovy.GroovyMarkupConfig;
import org.springframework.web.servlet.view.groovy.GroovyMarkupConfigurer;
import org.springframework.web.servlet.view.groovy.GroovyMarkupViewResolver;

import javax.annotation.PostConstruct;
import javax.servlet.Servlet;
import java.security.CodeSource;
import java.security.ProtectionDomain;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(MarkupTemplateEngine.class)
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
@EnableConfigurationProperties(GroovyTemplateProperties.class)
public class GroovyTemplateAutoConfiguration {

    private static final Log logger = LogFactory.getLog(GroovyTemplateAutoConfiguration.class);

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(GroovyMarkupConfigurer.class)
    public static class GroovyMarkupConfiguration {

        private final ApplicationContext applicationContext;

        private final GroovyTemplateProperties properties;

        public GroovyMarkupConfiguration(ApplicationContext applicationContext, GroovyTemplateProperties properties, ObjectProvider<MarkupTemplateEngine> templateEngine) {
            this.applicationContext = applicationContext;
            this.properties = properties;
        }

        @PostConstruct
        public void checkTemplateLocationExists() {
            if (this.properties.isCheckTemplateLocation() && !isUsingGroovyAllJar()) {
                TemplateLocation location = new TemplateLocation(this.properties.getResourceLoaderPath());
                if (!location.exists(this.applicationContext)) {
                    logger.warn("Cannot find template location: " + location + " (please add some templates, check your Groovy " + "configuration, or set spring.groovy.template." + "check-template-location=false)");
                }
            }
        }

        private boolean isUsingGroovyAllJar() {
            try {
                ProtectionDomain domain = MarkupTemplateEngine.class.getProtectionDomain();
                CodeSource codeSource = domain.getCodeSource();
                if (codeSource != null && codeSource.getLocation().toString().contains("-all")) {
                    return true;
                }
                return false;
            } catch (Exception ex) {
                return false;
            }
        }

        @Bean
        @ConditionalOnMissingBean(GroovyMarkupConfig.class)
        @ConfigurationProperties(prefix = "spring.groovy.template.configuration")
        public GroovyMarkupConfigurer groovyMarkupConfigurer(ObjectProvider<MarkupTemplateEngine> templateEngine) {
            GroovyMarkupConfigurer configurer = new GroovyMarkupConfigurer();
            configurer.setResourceLoaderPath(this.properties.getResourceLoaderPath());
            configurer.setCacheTemplates(this.properties.isCache());
            templateEngine.ifAvailable(configurer::setTemplateEngine);
            return configurer;
        }

    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({Servlet.class, LocaleContextHolder.class, UrlBasedViewResolver.class})
    @ConditionalOnWebApplication(type = Type.SERVLET)
    @ConditionalOnProperty(name = "spring.groovy.template.enabled", matchIfMissing = true)
    public static class GroovyWebConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "groovyMarkupViewResolver")
        public GroovyMarkupViewResolver groovyMarkupViewResolver(GroovyTemplateProperties properties) {
            GroovyMarkupViewResolver resolver = new GroovyMarkupViewResolver();
            properties.applyToMvcViewResolver(resolver);
            return resolver;
        }

    }

}
