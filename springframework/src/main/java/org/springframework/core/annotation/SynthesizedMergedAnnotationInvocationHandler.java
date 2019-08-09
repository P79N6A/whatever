package org.springframework.core.annotation;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;

final class SynthesizedMergedAnnotationInvocationHandler<A extends Annotation> implements InvocationHandler {

    private final MergedAnnotation<?> annotation;

    private final Class<A> type;

    private final AttributeMethods attributes;

    @Nullable
    private volatile Integer hashCode;

    private SynthesizedMergedAnnotationInvocationHandler(MergedAnnotation<A> annotation, Class<A> type) {
        Assert.notNull(annotation, "MergedAnnotation must not be null");
        Assert.notNull(type, "Type must not be null");
        Assert.isTrue(type.isAnnotation(), "Type must be an annotation");
        this.annotation = annotation;
        this.type = type;
        this.attributes = AttributeMethods.forAnnotationType(type);
        for (int i = 0; i < this.attributes.size(); i++) {
            getAttributeValue(this.attributes.get(i));
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        if (ReflectionUtils.isEqualsMethod(method)) {
            return annotationEquals(args[0]);
        }
        if (ReflectionUtils.isHashCodeMethod(method)) {
            return annotationHashCode();
        }
        if (ReflectionUtils.isToStringMethod(method)) {
            return this.annotation.toString();
        }
        if (isAnnotationTypeMethod(method)) {
            return this.type;
        }
        if (this.attributes.indexOf(method.getName()) != -1) {
            return getAttributeValue(method);
        }
        throw new AnnotationConfigurationException(String.format("Method [%s] is unsupported for synthesized annotation type [%s]", method, this.type));
    }

    private boolean isAnnotationTypeMethod(Method method) {
        return (Objects.equals(method.getName(), "annotationType") && method.getParameterCount() == 0);
    }

    private boolean annotationEquals(Object other) {
        if (this == other) {
            return true;
        }
        if (!this.type.isInstance(other)) {
            return false;
        }
        for (int i = 0; i < this.attributes.size(); i++) {
            Method attribute = this.attributes.get(i);
            Object thisValue = getAttributeValue(attribute);
            Object otherValue = ReflectionUtils.invokeMethod(attribute, other);
            if (!ObjectUtils.nullSafeEquals(thisValue, otherValue)) {
                return false;
            }
        }
        return true;
    }

    private int annotationHashCode() {
        Integer hashCode = this.hashCode;
        if (hashCode == null) {
            hashCode = computeHashCode();
            this.hashCode = hashCode;
        }
        return hashCode;
    }

    private Integer computeHashCode() {
        int hashCode = 0;
        for (int i = 0; i < this.attributes.size(); i++) {
            Method attribute = this.attributes.get(i);
            Object value = getAttributeValue(attribute);
            hashCode += (127 * attribute.getName().hashCode()) ^ getValueHashCode(value);
        }
        return hashCode;
    }

    private int getValueHashCode(Object value) {
        // Use Arrays.hashCode since ObjectUtils doesn't comply to to
        // Annotation#hashCode()
        if (value instanceof boolean[]) {
            return Arrays.hashCode((boolean[]) value);
        }
        if (value instanceof byte[]) {
            return Arrays.hashCode((byte[]) value);
        }
        if (value instanceof char[]) {
            return Arrays.hashCode((char[]) value);
        }
        if (value instanceof double[]) {
            return Arrays.hashCode((double[]) value);
        }
        if (value instanceof float[]) {
            return Arrays.hashCode((float[]) value);
        }
        if (value instanceof int[]) {
            return Arrays.hashCode((int[]) value);
        }
        if (value instanceof long[]) {
            return Arrays.hashCode((long[]) value);
        }
        if (value instanceof short[]) {
            return Arrays.hashCode((short[]) value);
        }
        if (value instanceof Object[]) {
            return Arrays.hashCode((Object[]) value);
        }
        return value.hashCode();
    }

    private Object getAttributeValue(Method method) {
        String name = method.getName();
        Class<?> type = ClassUtils.resolvePrimitiveIfNecessary(method.getReturnType());
        return this.annotation.getValue(name, type).orElseThrow(() -> new NoSuchElementException("No value found for attribute named '" + name + "' in merged annotation " + this.annotation.getType().getName()));
    }

    @SuppressWarnings("unchecked")
    static <A extends Annotation> A createProxy(MergedAnnotation<A> annotation, Class<A> type) {
        ClassLoader classLoader = type.getClassLoader();
        InvocationHandler handler = new SynthesizedMergedAnnotationInvocationHandler<>(annotation, type);
        Class<?>[] interfaces = isVisible(classLoader, SynthesizedAnnotation.class) ? new Class<?>[]{type, SynthesizedAnnotation.class} : new Class<?>[]{type};
        return (A) Proxy.newProxyInstance(classLoader, interfaces, handler);
    }

    private static boolean isVisible(ClassLoader classLoader, Class<?> interfaceClass) {
        try {
            return Class.forName(interfaceClass.getName(), false, classLoader) == interfaceClass;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

}
