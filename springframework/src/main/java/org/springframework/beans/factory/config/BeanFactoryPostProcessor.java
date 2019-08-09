package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;

@FunctionalInterface
public interface BeanFactoryPostProcessor {

    /**
     * 在BeanFactory标准初始化之后对其进行修改
     * 此时所有Bean定义已经被加载，但是还没有Bean被实例化
     * 对Bean的属性进行重写或者增加修改
     */
    void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException;

}
