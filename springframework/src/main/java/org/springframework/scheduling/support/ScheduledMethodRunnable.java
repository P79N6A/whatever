package org.springframework.scheduling.support;

import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;

public class ScheduledMethodRunnable implements Runnable {

    private final Object target;

    private final Method method;

    public ScheduledMethodRunnable(Object target, Method method) {
        this.target = target;
        this.method = method;
    }

    public ScheduledMethodRunnable(Object target, String methodName) throws NoSuchMethodException {
        this.target = target;
        this.method = target.getClass().getMethod(methodName);
    }

    public Object getTarget() {
        return this.target;
    }

    public Method getMethod() {
        return this.method;
    }

    @Override
    public void run() {
        try {
            ReflectionUtils.makeAccessible(this.method);
            this.method.invoke(this.target);
        } catch (InvocationTargetException ex) {
            ReflectionUtils.rethrowRuntimeException(ex.getTargetException());
        } catch (IllegalAccessException ex) {
            throw new UndeclaredThrowableException(ex);
        }
    }

    @Override
    public String toString() {
        return this.method.getDeclaringClass().getName() + "." + this.method.getName();
    }

}
