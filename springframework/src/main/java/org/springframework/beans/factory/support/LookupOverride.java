package org.springframework.beans.factory.support;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class LookupOverride extends MethodOverride {

    @Nullable
    private final String beanName;

    @Nullable
    private Method method;

    public LookupOverride(String methodName, @Nullable String beanName) {
        super(methodName);
        this.beanName = beanName;
    }

    public LookupOverride(Method method, @Nullable String beanName) {
        super(method.getName());
        this.method = method;
        this.beanName = beanName;
    }

    @Nullable
    public String getBeanName() {
        return this.beanName;
    }

    @Override
    public boolean matches(Method method) {
        if (this.method != null) {
            return method.equals(this.method);
        } else {
            return (method.getName().equals(getMethodName()) && (!isOverloaded() || Modifier.isAbstract(method.getModifiers()) || method.getParameterCount() == 0));
        }
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof LookupOverride) || !super.equals(other)) {
            return false;
        }
        LookupOverride that = (LookupOverride) other;
        return (ObjectUtils.nullSafeEquals(this.method, that.method) && ObjectUtils.nullSafeEquals(this.beanName, that.beanName));
    }

    @Override
    public int hashCode() {
        return (29 * super.hashCode() + ObjectUtils.nullSafeHashCode(this.beanName));
    }

    @Override
    public String toString() {
        return "LookupOverride for method '" + getMethodName() + "'";
    }

}
