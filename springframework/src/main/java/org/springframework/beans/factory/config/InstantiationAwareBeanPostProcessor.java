package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.lang.Nullable;

import java.beans.PropertyDescriptor;

/**
 * InstantiationAwareBeanPostProcessor：postProcessBeforeInstantiation -- Instantiation 实例化
 * BeanPostProcessor：postProcessBeforeInitialization -- Initialization 初始化（实例化并注入）
 */
public interface InstantiationAwareBeanPostProcessor extends BeanPostProcessor {

    /**
     * AbstractAutowireCapableBeanFactory#createBean
     */
    @Nullable
    default Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
        return null;
    }

    default boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
        return true;
    }

    @Nullable
    default PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) throws BeansException {
        return null;
    }

    @Deprecated
    @Nullable
    default PropertyValues postProcessPropertyValues(PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeansException {
        return pvs;
    }

}
