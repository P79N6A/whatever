package org.apache.dubbo.common.utils;

import java.lang.reflect.Method;

public class ClassHelper {
    public static Class<?> forNameWithThreadContextClassLoader(String name) throws ClassNotFoundException {
        return ClassUtils.forName(name, Thread.currentThread().getContextClassLoader());
    }

    public static Class<?> forNameWithCallerClassLoader(String name, Class<?> caller) throws ClassNotFoundException {
        return ClassUtils.forName(name, caller.getClassLoader());
    }

    public static ClassLoader getCallerClassLoader(Class<?> caller) {
        return caller.getClassLoader();
    }

    public static ClassLoader getClassLoader(Class<?> clazz) {
        return ClassUtils.getClassLoader(clazz);
    }

    public static ClassLoader getClassLoader() {
        return getClassLoader(ClassHelper.class);
    }

    public static Class<?> forName(String name) throws ClassNotFoundException {
        return forName(name, getClassLoader());
    }

    public static Class<?> forName(String name, ClassLoader classLoader) throws ClassNotFoundException, LinkageError {
        return ClassUtils.forName(name, classLoader);
    }

    public static Class<?> resolvePrimitiveClassName(String name) {
        return ClassUtils.resolvePrimitiveClassName(name);
    }

    public static String toShortString(Object obj) {
        return ClassUtils.toShortString(obj);

    }

    public static String simpleClassName(Class<?> clazz) {
        return ClassUtils.simpleClassName(clazz);
    }

    public static boolean isSetter(Method method) {
        return MethodUtils.isSetter(method);
    }

    public static boolean isGetter(Method method) {
        return MethodUtils.isGetter(method);
    }

    public static boolean isPrimitive(Class<?> type) {
        return ClassUtils.isPrimitive(type);
    }

    public static Object convertPrimitive(Class<?> type, String value) {
        return ClassUtils.convertPrimitive(type, value);
    }

    public static boolean isTypeMatch(Class<?> type, String value) {
        return ClassUtils.isTypeMatch(type, value);
    }

}
