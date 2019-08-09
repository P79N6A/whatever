package org.springframework.beans.factory.config;

import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.support.ArgumentConvertingMethodInvoker;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

import java.lang.reflect.InvocationTargetException;

public class MethodInvokingBean extends ArgumentConvertingMethodInvoker implements BeanClassLoaderAware, BeanFactoryAware, InitializingBean {

    @Nullable
    private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

    @Nullable
    private ConfigurableBeanFactory beanFactory;

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.beanClassLoader = classLoader;
    }

    @Override
    protected Class<?> resolveClassName(String className) throws ClassNotFoundException {
        return ClassUtils.forName(className, this.beanClassLoader);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        if (beanFactory instanceof ConfigurableBeanFactory) {
            this.beanFactory = (ConfigurableBeanFactory) beanFactory;
        }
    }

    @Override
    protected TypeConverter getDefaultTypeConverter() {
        if (this.beanFactory != null) {
            return this.beanFactory.getTypeConverter();
        } else {
            return super.getDefaultTypeConverter();
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        prepare();
        invokeWithTargetException();
    }

    @Nullable
    protected Object invokeWithTargetException() throws Exception {
        try {
            return invoke();
        } catch (InvocationTargetException ex) {
            if (ex.getTargetException() instanceof Exception) {
                throw (Exception) ex.getTargetException();
            }
            if (ex.getTargetException() instanceof Error) {
                throw (Error) ex.getTargetException();
            }
            throw ex;
        }
    }

}
