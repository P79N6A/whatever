package org.springframework.beans.factory.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.*;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public abstract class AbstractFactoryBean<T> implements FactoryBean<T>, BeanClassLoaderAware, BeanFactoryAware, InitializingBean, DisposableBean {

    protected final Log logger = LogFactory.getLog(getClass());

    private boolean singleton = true;

    @Nullable
    private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

    @Nullable
    private BeanFactory beanFactory;

    private boolean initialized = false;

    @Nullable
    private T singletonInstance;

    @Nullable
    private T earlySingletonInstance;

    public void setSingleton(boolean singleton) {
        this.singleton = singleton;
    }

    @Override
    public boolean isSingleton() {
        return this.singleton;
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.beanClassLoader = classLoader;
    }

    @Override
    public void setBeanFactory(@Nullable BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Nullable
    protected BeanFactory getBeanFactory() {
        return this.beanFactory;
    }

    protected TypeConverter getBeanTypeConverter() {
        BeanFactory beanFactory = getBeanFactory();
        if (beanFactory instanceof ConfigurableBeanFactory) {
            return ((ConfigurableBeanFactory) beanFactory).getTypeConverter();
        } else {
            return new SimpleTypeConverter();
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (isSingleton()) {
            this.initialized = true;
            this.singletonInstance = createInstance();
            this.earlySingletonInstance = null;
        }
    }

    @Override
    public final T getObject() throws Exception {
        if (isSingleton()) {
            return (this.initialized ? this.singletonInstance : getEarlySingletonInstance());
        } else {
            return createInstance();
        }
    }

    @SuppressWarnings("unchecked")
    private T getEarlySingletonInstance() throws Exception {
        Class<?>[] ifcs = getEarlySingletonInterfaces();
        if (ifcs == null) {
            throw new FactoryBeanNotInitializedException(getClass().getName() + " does not support circular references");
        }
        if (this.earlySingletonInstance == null) {
            this.earlySingletonInstance = (T) Proxy.newProxyInstance(this.beanClassLoader, ifcs, new EarlySingletonInvocationHandler());
        }
        return this.earlySingletonInstance;
    }

    @Nullable
    private T getSingletonInstance() throws IllegalStateException {
        Assert.state(this.initialized, "Singleton instance not initialized yet");
        return this.singletonInstance;
    }

    @Override
    public void destroy() throws Exception {
        if (isSingleton()) {
            destroyInstance(this.singletonInstance);
        }
    }

    @Override
    @Nullable
    public abstract Class<?> getObjectType();

    protected abstract T createInstance() throws Exception;

    @Nullable
    protected Class<?>[] getEarlySingletonInterfaces() {
        Class<?> type = getObjectType();
        return (type != null && type.isInterface() ? new Class<?>[]{type} : null);
    }

    protected void destroyInstance(@Nullable T instance) throws Exception {
    }

    private class EarlySingletonInvocationHandler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (ReflectionUtils.isEqualsMethod(method)) {
                // Only consider equal when proxies are identical.
                return (proxy == args[0]);
            } else if (ReflectionUtils.isHashCodeMethod(method)) {
                // Use hashCode of reference proxy.
                return System.identityHashCode(proxy);
            } else if (!initialized && ReflectionUtils.isToStringMethod(method)) {
                return "Early singleton proxy for interfaces " + ObjectUtils.nullSafeToString(getEarlySingletonInterfaces());
            }
            try {
                return method.invoke(getSingletonInstance(), args);
            } catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            }
        }

    }

}
