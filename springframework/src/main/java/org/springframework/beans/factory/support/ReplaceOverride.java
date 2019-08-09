package org.springframework.beans.factory.support;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

public class ReplaceOverride extends MethodOverride {

    private final String methodReplacerBeanName;

    private List<String> typeIdentifiers = new LinkedList<>();

    public ReplaceOverride(String methodName, String methodReplacerBeanName) {
        super(methodName);
        Assert.notNull(methodName, "Method replacer bean name must not be null");
        this.methodReplacerBeanName = methodReplacerBeanName;
    }

    public String getMethodReplacerBeanName() {
        return this.methodReplacerBeanName;
    }

    public void addTypeIdentifier(String identifier) {
        this.typeIdentifiers.add(identifier);
    }

    @Override
    public boolean matches(Method method) {
        if (!method.getName().equals(getMethodName())) {
            return false;
        }
        if (!isOverloaded()) {
            // Not overloaded: don't worry about arg type matching...
            return true;
        }
        // If we get here, we need to insist on precise argument matching...
        if (this.typeIdentifiers.size() != method.getParameterCount()) {
            return false;
        }
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < this.typeIdentifiers.size(); i++) {
            String identifier = this.typeIdentifiers.get(i);
            if (!parameterTypes[i].getName().contains(identifier)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ReplaceOverride) || !super.equals(other)) {
            return false;
        }
        ReplaceOverride that = (ReplaceOverride) other;
        return (ObjectUtils.nullSafeEquals(this.methodReplacerBeanName, that.methodReplacerBeanName) && ObjectUtils.nullSafeEquals(this.typeIdentifiers, that.typeIdentifiers));
    }

    @Override
    public int hashCode() {
        int hashCode = super.hashCode();
        hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.methodReplacerBeanName);
        hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.typeIdentifiers);
        return hashCode;
    }

    @Override
    public String toString() {
        return "Replace override for method '" + getMethodName() + "'";
    }

}
