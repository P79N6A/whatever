package org.springframework.aop.aspectj;

import org.springframework.aop.framework.AopConfigException;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;

public class SimpleAspectInstanceFactory implements AspectInstanceFactory {

    private final Class<?> aspectClass;

    public SimpleAspectInstanceFactory(Class<?> aspectClass) {
        Assert.notNull(aspectClass, "Aspect class must not be null");
        this.aspectClass = aspectClass;
    }

    public final Class<?> getAspectClass() {
        return this.aspectClass;
    }

    @Override
    public final Object getAspectInstance() {
        try {
            return ReflectionUtils.accessibleConstructor(this.aspectClass).newInstance();
        } catch (NoSuchMethodException ex) {
            throw new AopConfigException("No default constructor on aspect class: " + this.aspectClass.getName(), ex);
        } catch (InstantiationException ex) {
            throw new AopConfigException("Unable to instantiate aspect class: " + this.aspectClass.getName(), ex);
        } catch (IllegalAccessException ex) {
            throw new AopConfigException("Could not access aspect constructor: " + this.aspectClass.getName(), ex);
        } catch (InvocationTargetException ex) {
            throw new AopConfigException("Failed to invoke aspect constructor: " + this.aspectClass.getName(), ex.getTargetException());
        }
    }

    @Override
    @Nullable
    public ClassLoader getAspectClassLoader() {
        return this.aspectClass.getClassLoader();
    }

    @Override
    public int getOrder() {
        return getOrderForAspectClass(this.aspectClass);
    }

    protected int getOrderForAspectClass(Class<?> aspectClass) {
        return Ordered.LOWEST_PRECEDENCE;
    }

}
