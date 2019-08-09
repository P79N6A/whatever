package org.springframework.aop.config;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class AopNamespaceHandler extends NamespaceHandlerSupport {

    @Override
    public void init() {
        registerBeanDefinitionParser("config", new ConfigBeanDefinitionParser());
        registerBeanDefinitionParser("aspectj-autoproxy", new AspectJAutoProxyBeanDefinitionParser());
        registerBeanDefinitionDecorator("scoped-proxy", new ScopedProxyBeanDefinitionDecorator());
        registerBeanDefinitionParser("spring-configured", new SpringConfiguredBeanDefinitionParser());
    }

}
