package org.springframework.aop.config;

import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.*;
import org.springframework.beans.factory.xml.BeanDefinitionDecorator;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.w3c.dom.Node;

import java.util.List;

public abstract class AbstractInterceptorDrivenBeanDefinitionDecorator implements BeanDefinitionDecorator {

    @Override
    public final BeanDefinitionHolder decorate(Node node, BeanDefinitionHolder definitionHolder, ParserContext parserContext) {
        BeanDefinitionRegistry registry = parserContext.getRegistry();
        String existingBeanName = definitionHolder.getBeanName();
        BeanDefinition targetDefinition = definitionHolder.getBeanDefinition();
        BeanDefinitionHolder targetHolder = new BeanDefinitionHolder(targetDefinition, existingBeanName + ".TARGET");
        BeanDefinition interceptorDefinition = createInterceptorDefinition(node);
        String interceptorName = existingBeanName + '.' + getInterceptorNameSuffix(interceptorDefinition);
        BeanDefinitionReaderUtils.registerBeanDefinition(new BeanDefinitionHolder(interceptorDefinition, interceptorName), registry);
        BeanDefinitionHolder result = definitionHolder;
        if (!isProxyFactoryBeanDefinition(targetDefinition)) {
            RootBeanDefinition proxyDefinition = new RootBeanDefinition();
            proxyDefinition.setBeanClass(ProxyFactoryBean.class);
            proxyDefinition.setScope(targetDefinition.getScope());
            proxyDefinition.setLazyInit(targetDefinition.isLazyInit());
            proxyDefinition.setDecoratedDefinition(targetHolder);
            proxyDefinition.getPropertyValues().add("target", targetHolder);
            proxyDefinition.getPropertyValues().add("interceptorNames", new ManagedList<String>());
            proxyDefinition.setAutowireCandidate(targetDefinition.isAutowireCandidate());
            proxyDefinition.setPrimary(targetDefinition.isPrimary());
            if (targetDefinition instanceof AbstractBeanDefinition) {
                proxyDefinition.copyQualifiersFrom((AbstractBeanDefinition) targetDefinition);
            }
            result = new BeanDefinitionHolder(proxyDefinition, existingBeanName);
        }
        addInterceptorNameToList(interceptorName, result.getBeanDefinition());
        return result;
    }

    @SuppressWarnings("unchecked")
    private void addInterceptorNameToList(String interceptorName, BeanDefinition beanDefinition) {
        List<String> list = (List<String>) beanDefinition.getPropertyValues().get("interceptorNames");
        Assert.state(list != null, "Missing 'interceptorNames' property");
        list.add(interceptorName);
    }

    private boolean isProxyFactoryBeanDefinition(BeanDefinition existingDefinition) {
        return ProxyFactoryBean.class.getName().equals(existingDefinition.getBeanClassName());
    }

    protected String getInterceptorNameSuffix(BeanDefinition interceptorDefinition) {
        String beanClassName = interceptorDefinition.getBeanClassName();
        return (StringUtils.hasLength(beanClassName) ? StringUtils.uncapitalize(ClassUtils.getShortName(beanClassName)) : "");
    }

    protected abstract BeanDefinition createInterceptorDefinition(Node node);

}
