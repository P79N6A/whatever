package org.springframework.aop.support.annotation;

import org.springframework.aop.ClassFilter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;

import java.lang.annotation.Annotation;

public class AnnotationClassFilter implements ClassFilter {

    private final Class<? extends Annotation> annotationType;

    private final boolean checkInherited;

    public AnnotationClassFilter(Class<? extends Annotation> annotationType) {
        this(annotationType, false);
    }

    public AnnotationClassFilter(Class<? extends Annotation> annotationType, boolean checkInherited) {
        Assert.notNull(annotationType, "Annotation type must not be null");
        this.annotationType = annotationType;
        this.checkInherited = checkInherited;
    }

    @Override
    public boolean matches(Class<?> clazz) {
        return (this.checkInherited ? AnnotatedElementUtils.hasAnnotation(clazz, this.annotationType) : clazz.isAnnotationPresent(this.annotationType));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AnnotationClassFilter)) {
            return false;
        }
        AnnotationClassFilter otherCf = (AnnotationClassFilter) other;
        return (this.annotationType.equals(otherCf.annotationType) && this.checkInherited == otherCf.checkInherited);
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
