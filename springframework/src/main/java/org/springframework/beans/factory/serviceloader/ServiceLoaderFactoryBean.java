package org.springframework.beans.factory.serviceloader;

import org.springframework.beans.factory.BeanClassLoaderAware;

import java.util.ServiceLoader;

public class ServiceLoaderFactoryBean extends AbstractServiceLoaderBasedFactoryBean implements BeanClassLoaderAware {

    @Override
    protected Object getObjectToExpose(ServiceLoader<?> serviceLoader) {
        return serviceLoader;
    }

    @Override
    public Class<?> getObjectType() {
        return ServiceLoader.class;
    }

}
