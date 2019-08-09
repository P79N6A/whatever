package org.springframework.boot.context.properties;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.util.ClassUtils;

public class ConfigurationPropertiesBindException extends BeanCreationException {

    private final Class<?> beanType;

    private final ConfigurationProperties annotation;

    ConfigurationPropertiesBindException(String beanName, Class<?> beanType, ConfigurationProperties annotation, Exception cause) {
        super(beanName, getMessage(beanType, annotation), cause);
        this.beanType = beanType;
        this.annotation = annotation;
    }

    public Class<?> getBeanType() {
        return this.beanType;
    }

    public ConfigurationProperties getAnnotation() {
        return this.annotation;
    }

    private static String getMessage(Class<?> beanType, ConfigurationProperties annotation) {
        StringBuilder message = new StringBuilder();
        message.append("Could not bind properties to '");
        message.append(ClassUtils.getShortName(beanType)).append("' : ");
        message.append("prefix=").append(annotation.prefix());
        message.append(", ignoreInvalidFields=").append(annotation.ignoreInvalidFields());
        message.append(", ignoreUnknownFields=").append(annotation.ignoreUnknownFields());
        return message.toString();
    }

}
