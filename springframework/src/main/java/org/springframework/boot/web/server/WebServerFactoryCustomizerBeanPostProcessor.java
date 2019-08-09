package org.springframework.boot.web.server;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.util.LambdaSafe;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class WebServerFactoryCustomizerBeanPostProcessor implements BeanPostProcessor, BeanFactoryAware {

    private ListableBeanFactory beanFactory;

    private List<WebServerFactoryCustomizer<?>> customizers;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        Assert.isInstanceOf(ListableBeanFactory.class, beanFactory, "WebServerCustomizerBeanPostProcessor can only be used " + "with a ListableBeanFactory");
        this.beanFactory = (ListableBeanFactory) beanFactory;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof WebServerFactory) {
            // 查找容器中所有类型为WebServerFactoryCustomizer的Bean，调用WebServerFactoryCustomizer#customize设置
            postProcessBeforeInitialization((WebServerFactory) bean);
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @SuppressWarnings("unchecked")
    private void postProcessBeforeInitialization(WebServerFactory webServerFactory) {
        LambdaSafe.callbacks(WebServerFactoryCustomizer.class, getCustomizers(), webServerFactory).withLogger(WebServerFactoryCustomizerBeanPostProcessor.class).invoke((customizer) -> customizer.customize(webServerFactory));
    }

    private Collection<WebServerFactoryCustomizer<?>> getCustomizers() {
        if (this.customizers == null) {
            // Look up does not include the parent context
            this.customizers = new ArrayList<>(getWebServerFactoryCustomizerBeans());
            this.customizers.sort(AnnotationAwareOrderComparator.INSTANCE);
            this.customizers = Collections.unmodifiableList(this.customizers);
        }
        return this.customizers;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Collection<WebServerFactoryCustomizer<?>> getWebServerFactoryCustomizerBeans() {
        return (Collection) this.beanFactory.getBeansOfType(WebServerFactoryCustomizer.class, false, false).values();
    }

}
