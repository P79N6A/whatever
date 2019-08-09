package org.springframework.boot.context.properties;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ConfigurationBeanFactoryMetadata implements ApplicationContextAware {

    public static final String BEAN_NAME = ConfigurationBeanFactoryMetadata.class.getName();

    private ConfigurableApplicationContext applicationContext;

    public <A extends Annotation> Map<String, Object> getBeansWithFactoryAnnotation(Class<A> type) {
        Map<String, Object> result = new HashMap<>();
        for (String name : this.applicationContext.getBeanFactory().getBeanDefinitionNames()) {
            if (findFactoryAnnotation(name, type) != null) {
                result.put(name, this.applicationContext.getBean(name));
            }
        }
        return result;
    }

    public <A extends Annotation> A findFactoryAnnotation(String beanName, Class<A> type) {
        Method method = findFactoryMethod(beanName);
        return (method != null) ? AnnotationUtils.findAnnotation(method, type) : null;
    }

    public Method findFactoryMethod(String beanName) {
        ConfigurableListableBeanFactory beanFactory = this.applicationContext.getBeanFactory();
        if (beanFactory.containsBeanDefinition(beanName)) {
            BeanDefinition beanDefinition = beanFactory.getMergedBeanDefinition(beanName);
            if (beanDefinition instanceof RootBeanDefinition) {
                return ((RootBeanDefinition) beanDefinition).getResolvedFactoryMethod();
            }
        }
        return null;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = (ConfigurableApplicationContext) applicationContext;
    }

}
