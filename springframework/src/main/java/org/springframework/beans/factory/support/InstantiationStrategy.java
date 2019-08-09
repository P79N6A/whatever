package org.springframework.beans.factory.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.lang.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public interface InstantiationStrategy {

    Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner) throws BeansException;

    Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner, Constructor<?> ctor, Object... args) throws BeansException;

    Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner, @Nullable Object factoryBean, Method factoryMethod, Object... args) throws BeansException;

}
