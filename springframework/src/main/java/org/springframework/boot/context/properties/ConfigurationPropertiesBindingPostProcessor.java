package org.springframework.boot.context.properties;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.validation.annotation.Validated;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class ConfigurationPropertiesBindingPostProcessor implements BeanPostProcessor, PriorityOrdered, ApplicationContextAware, InitializingBean {

    public static final String BEAN_NAME = ConfigurationPropertiesBindingPostProcessor.class.getName();

    @Deprecated
    public static final String VALIDATOR_BEAN_NAME = ConfigurationPropertiesBindingPostProcessorRegistrar.VALIDATOR_BEAN_NAME;

    private ConfigurationBeanFactoryMetadata beanFactoryMetadata;

    private ApplicationContext applicationContext;

    private ConfigurationPropertiesBinder configurationPropertiesBinder;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // We can't use constructor injection of the application context because
        // it causes eager factory bean initialization
        this.beanFactoryMetadata = this.applicationContext.getBean(ConfigurationBeanFactoryMetadata.BEAN_NAME, ConfigurationBeanFactoryMetadata.class);
        this.configurationPropertiesBinder = this.applicationContext.getBean(ConfigurationPropertiesBinder.BEAN_NAME, ConfigurationPropertiesBinder.class);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        ConfigurationProperties annotation = getAnnotation(bean, beanName, ConfigurationProperties.class);
        // 被@ConfigurationProperties注解 && 还没被注入
        if (annotation != null && !hasBeenBound(beanName)) {
            // 注入
            bind(bean, beanName, annotation);
        }
        return bean;
    }

    private boolean hasBeenBound(String beanName) {
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) this.applicationContext.getAutowireCapableBeanFactory();
        if (registry.containsBeanDefinition(beanName)) {
            BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
            return beanDefinition instanceof ConfigurationPropertiesBeanDefinition;
        }
        return false;
    }

    private void bind(Object bean, String beanName, ConfigurationProperties annotation) {
        ResolvableType type = getBeanType(bean, beanName);
        Validated validated = getAnnotation(bean, beanName, Validated.class);
        Annotation[] annotations = (validated != null) ? new Annotation[]{annotation, validated} : new Annotation[]{annotation};
        Bindable<?> target = Bindable.of(type).withExistingValue(bean).withAnnotations(annotations);
        try {
            // 交给之前注册的ConfigurationPropertiesBinder
            this.configurationPropertiesBinder.bind(target);
        } catch (Exception ex) {
            throw new ConfigurationPropertiesBindException(beanName, bean.getClass(), annotation, ex);
        }
    }

    private ResolvableType getBeanType(Object bean, String beanName) {
        Method factoryMethod = this.beanFactoryMetadata.findFactoryMethod(beanName);
        if (factoryMethod != null) {
            return ResolvableType.forMethodReturnType(factoryMethod);
        }
        return ResolvableType.forClass(bean.getClass());
    }

    private <A extends Annotation> A getAnnotation(Object bean, String beanName, Class<A> type) {
        A annotation = this.beanFactoryMetadata.findFactoryAnnotation(beanName, type);
        if (annotation == null) {
            annotation = AnnotationUtils.findAnnotation(bean.getClass(), type);
        }
        return annotation;
    }

}
