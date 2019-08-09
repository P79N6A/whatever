package org.springframework.aop.support;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("serial")
public class ControlFlowPointcut implements Pointcut, ClassFilter, MethodMatcher, Serializable {

    private Class<?> clazz;

    @Nullable
    private String methodName;

    private final AtomicInteger evaluations = new AtomicInteger(0);

    public ControlFlowPointcut(Class<?> clazz) {
        this(clazz, null);
    }

    public ControlFlowPointcut(Class<?> clazz, @Nullable String methodName) {
        Assert.notNull(clazz, "Class must not be null");
        this.clazz = clazz;
        this.methodName = methodName;
    }

    @Override
    public boolean matches(Class<?> clazz) {
        return true;
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        return true;
    }

    @Override
    public boolean isRuntime() {
        return true;
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass, Object... args) {
        this.evaluations.incrementAndGet();
        for (StackTraceElement element : new Throwable().getStackTrace()) {
            if (element.getClassName().equals(this.clazz.getName()) && (this.methodName == null || element.getMethodName().equals(this.methodName))) {
                return true;
            }
        }
        return false;
    }

    public int getEvaluations() {
        return this.evaluations.get();
    }

    @Override
    public ClassFilter getClassFilter() {
        return this;
    }

    @Override
    public MethodMatcher getMethodMatcher() {
        return this;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ControlFlowPointcut)) {
            return false;
        }
        ControlFlowPointcut that = (ControlFlowPointcut) other;
        return (this.clazz.equals(that.clazz)) && ObjectUtils.nullSafeEquals(this.methodName, that.methodName);
    }

    @Override
    public int hashCode() {
        int code = this.clazz.hashCode();
        if (this.methodName != null) {
            code = 37 * code + this.methodName.hashCode();
        }
        return code;
    }

}
