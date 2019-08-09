package org.springframework.boot.autoconfigure.web.servlet;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.server.ErrorPageRegistrarBeanPostProcessor;
import org.springframework.boot.web.server.WebServerFactoryCustomizerBeanPostProcessor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.Ordered;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ObjectUtils;
import org.springframework.web.filter.ForwardedHeaderFilter;

import javax.servlet.DispatcherType;
import javax.servlet.ServletRequest;

@Configuration(proxyBeanMethods = false)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE) // 最高优先级
@ConditionalOnClass(ServletRequest.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
@EnableConfigurationProperties(ServerProperties.class) // 引入ServerProperties配置类，读取上下文的以server为开头的属性
@Import({
        // 注册WebServerFactoryCustomizerBeanPostProcessor，在postProcessBeforeInitialization设置容器
        ServletWebServerFactoryAutoConfiguration.BeanPostProcessorsRegistrar.class,
        // 注册Tomcat工厂
        ServletWebServerFactoryConfiguration.EmbeddedTomcat.class})
public class ServletWebServerFactoryAutoConfiguration {

    /**
     * 从ServerProperties配置Tomcat的Servlet信息，Port，Address，ContextPath，Ssl，Compression，Http2，ServerHeader等等
     */
    @Bean
    public ServletWebServerFactoryCustomizer servletWebServerFactoryCustomizer(ServerProperties serverProperties) {
        return new ServletWebServerFactoryCustomizer(serverProperties);
    }

    /**
     * 从ServerProperties配置Tomcat：RedirectContextRoot，UseRelativeRedirects等
     */
    @Bean
    @ConditionalOnClass(name = "org.apache.catalina.startup.Tomcat") // 存在这个类时才初始化
    public TomcatServletWebServerFactoryCustomizer tomcatServletWebServerFactoryCustomizer(ServerProperties serverProperties) {
        return new TomcatServletWebServerFactoryCustomizer(serverProperties);
    }

    @Bean
    @ConditionalOnMissingFilterBean(ForwardedHeaderFilter.class)
    @ConditionalOnProperty(value = "server.forward-headers-strategy", havingValue = "framework")
    public FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter() {
        ForwardedHeaderFilter filter = new ForwardedHeaderFilter();
        FilterRegistrationBean<ForwardedHeaderFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    public static class BeanPostProcessorsRegistrar implements ImportBeanDefinitionRegistrar, BeanFactoryAware {

        private ConfigurableListableBeanFactory beanFactory;

        @Override
        public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
            if (beanFactory instanceof ConfigurableListableBeanFactory) {
                this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
            }
        }

        @Override
        public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
            if (this.beanFactory == null) {
                return;
            }
            registerSyntheticBeanIfMissing(registry, "webServerFactoryCustomizerBeanPostProcessor", WebServerFactoryCustomizerBeanPostProcessor.class);
            registerSyntheticBeanIfMissing(registry, "errorPageRegistrarBeanPostProcessor", ErrorPageRegistrarBeanPostProcessor.class);
        }

        private void registerSyntheticBeanIfMissing(BeanDefinitionRegistry registry, String name, Class<?> beanClass) {
            if (ObjectUtils.isEmpty(this.beanFactory.getBeanNamesForType(beanClass, true, false))) {
                RootBeanDefinition beanDefinition = new RootBeanDefinition(beanClass);
                beanDefinition.setSynthetic(true);
                registry.registerBeanDefinition(name, beanDefinition);
            }
        }

    }

}
