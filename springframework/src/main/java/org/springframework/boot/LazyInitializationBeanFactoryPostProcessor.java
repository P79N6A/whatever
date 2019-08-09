package org.springframework.boot;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.core.Ordered;

public final class LazyInitializationBeanFactoryPostProcessor implements BeanFactoryPostProcessor, Ordered {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        for (String name : beanFactory.getBeanDefinitionNames()) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(name);
            if (beanDefinition instanceof AbstractBeanDefinition) {
                Boolean lazyInit = ((AbstractBeanDefinition) beanDefinition).getLazyInit();
                if (lazyInit != null && !lazyInit) {
                    continue;
                }
            }
            beanDefinition.setLazyInit(true);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

}
