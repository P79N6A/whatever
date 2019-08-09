package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;
import org.springframework.lang.Nullable;

import java.lang.reflect.Constructor;

public interface SmartInstantiationAwareBeanPostProcessor extends InstantiationAwareBeanPostProcessor {

    @Nullable
    default Class<?> predictBeanType(Class<?> beanClass, String beanName) throws BeansException {
        return null;
    }

    @Nullable
    default Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, String beanName) throws BeansException {
        return null;
    }

    default Object getEarlyBeanReference(Object bean, String beanName) throws BeansException {
        return bean;
    }

}
