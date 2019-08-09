package org.springframework.aop.support.annotation;

import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.StaticMethodMatcher;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class AnnotationMethodMatcher extends StaticMethodMatcher {

    private final Class<? extends Annotation> annotationType;

    private final boolean checkInherited;

    public AnnotationMethodMatcher(Class<? extends Annotation> annotationType) {
        this(annotationType, false);
    }

    public AnnotationMethodMatcher(Class<? extends Annotation> annotationType, boolean checkInherited) {
        Assert.notNull(annotationType, "Annotation type must not be null");
        this.annotationType = annotationType;
        this.checkInherited = checkInherited;
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        if (matchesMethod(method)) {
            return true;
        }
        if (Proxy.isProxyClass(targetClass)) {
            return false;
        }
        Method specificMethod = AopUtils.getMostSpecificMethod(method, targetClass);
        return (specificMethod != method && matchesMethod(specificMethod));
    }

    private boolean matchesMethod(Method method) {
        return (this.checkInherited ? AnnotatedElementUtils.hasAnnotation(method, this.annotationType) : method.isAnnotationPresent(this.annotationType));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AnnotationMethodMatcher)) {
            return false;
        }
        AnnotationMethodMatcher otherMm = (AnnotationMethodMatcher) other;
        return this.annotationType.equals(otherMm.annotationType);
    }

    @Override
    public int hashCode() {
        return this.annotationType.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getName() + ": " + this.annotationType;
    }

}
