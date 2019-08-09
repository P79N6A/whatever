package org.springframework.boot.web.servlet;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;

import javax.servlet.annotation.WebListener;
import java.util.Map;

class WebListenerHandler extends ServletComponentHandler {

    WebListenerHandler() {
        super(WebListener.class);
    }

    @Override
    protected void doHandle(Map<String, Object> attributes, ScannedGenericBeanDefinition beanDefinition, BeanDefinitionRegistry registry) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(ServletListenerRegistrationBean.class);
        builder.addPropertyValue("listener", beanDefinition);
        registry.registerBeanDefinition(beanDefinition.getBeanClassName(), builder.getBeanDefinition());
    }

}
