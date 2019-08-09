package org.springframework.boot.context.properties;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.validation.annotation.Validated;

import java.lang.annotation.Annotation;
import java.util.function.Supplier;

final class ConfigurationPropertiesBeanDefinition extends GenericBeanDefinition {

    static ConfigurationPropertiesBeanDefinition from(ConfigurableListableBeanFactory beanFactory, String beanName, Class<?> type) {
        ConfigurationPropertiesBeanDefinition beanDefinition = new ConfigurationPropertiesBeanDefinition();
        beanDefinition.setBeanClass(type);
        beanDefinition.setInstanceSupplier(createBean(beanFactory, beanName, type));
        return beanDefinition;
    }

    private static <T> Supplier<T> createBean(ConfigurableListableBeanFactory beanFactory, String beanName, Class<T> type) {
        return () -> {
            // FIXME review
            ConfigurationProperties annotation = getAnnotation(type, ConfigurationProperties.class);
            Validated validated = getAnnotation(type, Validated.class);
            Annotation[] annotations = (validated != null) ? new Annotation[]{annotation, validated} : new Annotation[]{annotation};
            Bindable<T> bindable = Bindable.of(type).withAnnotations(annotations);
            ConfigurationPropertiesBinder binder = beanFactory.getBean(ConfigurationPropertiesBinder.BEAN_NAME, ConfigurationPropertiesBinder.class);
            try {
                return binder.bind(bindable).orElseCreate(type);
            } catch (Exception ex) {
                throw new ConfigurationPropertiesBindException(beanName, type, annotation, ex);
            }
        };
    }

    private static <A extends Annotation> A getAnnotation(Class<?> type, Class<A> annotationType) {
        return AnnotationUtils.findAnnotation(type, annotationType);
    }

}
