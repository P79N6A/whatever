package org.springframework.scheduling.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.support.ArgumentConvertingMethodInvoker;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

import java.lang.reflect.InvocationTargetException;

public class MethodInvokingRunnable extends ArgumentConvertingMethodInvoker implements Runnable, BeanClassLoaderAware, InitializingBean {

    protected final Log logger = LogFactory.getLog(getClass());

    @Nullable
    private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.beanClassLoader = classLoader;
    }

    @Override
    protected Class<?> resolveClassName(String className) throws ClassNotFoundException {
        return ClassUtils.forName(className, this.beanClassLoader);
    }

    @Override
    public void afterPropertiesSet() throws ClassNotFoundException, NoSuchMethodException {
        prepare();
    }

    @Override
    public void run() {
        try {
            invoke();
        } catch (InvocationTargetException ex) {
            logger.error(getInvocationFailureMessage(), ex.getTargetException());
            // Do not throw exception, else the main loop of the scheduler might stop!
        } catch (Throwable ex) {
            logger.error(getInvocationFailureMessage(), ex);
            // Do not throw exception, else the main loop of the scheduler might stop!
        }
    }

    protected String getInvocationFailureMessage() {
        return "Invocation of method '" + getTargetMethod() + "' on target class [" + getTargetClass() + "] failed";
    }

}
