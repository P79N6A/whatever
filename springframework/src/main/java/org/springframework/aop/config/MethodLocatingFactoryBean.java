package org.springframework.aop.config;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

public class MethodLocatingFactoryBean implements FactoryBean<Method>, BeanFactoryAware {

    @Nullable
    private String targetBeanName;

    @Nullable
    private String methodName;

    @Nullable
    private Method method;

    public void setTargetBeanName(String targetBeanName) {
        this.targetBeanName = targetBeanName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        if (!StringUtils.hasText(this.targetBeanName)) {
            throw new IllegalArgumentException("Property 'targetBeanName' is required");
        }
        if (!StringUtils.hasText(this.methodName)) {
            throw new IllegalArgumentException("Property 'methodName' is required");
        }
        Class<?> beanClass = beanFactory.getType(this.targetBeanName);
        if (beanClass == null) {
            throw new IllegalArgumentException("Can't determine type of bean with name '" + this.targetBeanName + "'");
        }
        this.method = BeanUtils.resolveSignature(this.methodName, beanClass);
        if (this.method == null) {
            throw new IllegalArgumentException("Unable to locate method [" + this.methodName + "] on bean [" + this.targetBeanName + "]");
        }
    }

    @Override
    @Nullable
    public Method getObject() throws Exception {
        return this.method;
    }

    @Override
    public Class<Method> getObjectType() {
        return Method.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

}
