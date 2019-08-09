package org.springframework.beans.factory.serviceloader;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.lang.Nullable;

import java.util.Iterator;
import java.util.ServiceLoader;

public class ServiceFactoryBean extends AbstractServiceLoaderBasedFactoryBean implements BeanClassLoaderAware {

    @Override
    protected Object getObjectToExpose(ServiceLoader<?> serviceLoader) {
        Iterator<?> it = serviceLoader.iterator();
        if (!it.hasNext()) {
            throw new IllegalStateException("ServiceLoader could not find service for type [" + getServiceType() + "]");
        }
        return it.next();
    }

    @Override
    @Nullable
    public Class<?> getObjectType() {
        return getServiceType();
    }

}
