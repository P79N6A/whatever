package org.springframework.beans.factory.serviceloader;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.ServiceLoader;

public abstract class AbstractServiceLoaderBasedFactoryBean extends AbstractFactoryBean<Object> implements BeanClassLoaderAware {

    @Nullable
    private Class<?> serviceType;

    @Nullable
    private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

    public void setServiceType(@Nullable Class<?> serviceType) {
        this.serviceType = serviceType;
    }

    @Nullable
    public Class<?> getServiceType() {
        return this.serviceType;
    }

    @Override
    public void setBeanClassLoader(@Nullable ClassLoader beanClassLoader) {
        this.beanClassLoader = beanClassLoader;
    }

    @Override
    protected Object createInstance() {
        Assert.notNull(getServiceType(), "Property 'serviceType' is required");
        return getObjectToExpose(ServiceLoader.load(getServiceType(), this.beanClassLoader));
    }

    protected abstract Object getObjectToExpose(ServiceLoader<?> serviceLoader);

}
