package org.springframework.beans.factory.config;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.FactoryBeanNotInitializedException;
import org.springframework.lang.Nullable;

public class MethodInvokingFactoryBean extends MethodInvokingBean implements FactoryBean<Object> {

    private boolean singleton = true;

    private boolean initialized = false;

    @Nullable
    private Object singletonObject;

    public void setSingleton(boolean singleton) {
        this.singleton = singleton;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        prepare();
        if (this.singleton) {
            this.initialized = true;
            this.singletonObject = invokeWithTargetException();
        }
    }

    @Override
    @Nullable
    public Object getObject() throws Exception {
        if (this.singleton) {
            if (!this.initialized) {
                throw new FactoryBeanNotInitializedException();
            }
            // Singleton: return shared object.
            return this.singletonObject;
        } else {
            // Prototype: new object on each call.
            return invokeWithTargetException();
        }
    }

    @Override
    public Class<?> getObjectType() {
        if (!isPrepared()) {
            // Not fully initialized yet -> return null to indicate "not known yet".
            return null;
        }
        return getPreparedMethod().getReturnType();
    }

    @Override
    public boolean isSingleton() {
        return this.singleton;
    }

}
