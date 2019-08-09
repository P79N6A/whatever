package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;
import org.springframework.lang.Nullable;

/**
 * 在容器完成Bean初始化前后添加逻辑处理
 * 1. InstantiationAwareBeanPostProcessor
 * 2. MergedBeanDefinitionPostProcessor
 * 3. DestructionAwareBeanPostProcessor
 * 4. SmartInstantiationAwareBeanPostProcessor
 * 5. BeanPostProcessor
 */
public interface BeanPostProcessor {

    /**
     * 初始化之前
     */
    @Nullable
    default Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    /**
     * 初始化之后
     */
    @Nullable
    default Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

}
