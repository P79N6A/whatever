package org.springframework.core;

import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class MethodIntrospector {

    private MethodIntrospector() {
    }

    public static <T> Map<Method, T> selectMethods(Class<?> targetType, final MetadataLookup<T> metadataLookup) {
        final Map<Method, T> methodMap = new LinkedHashMap<>();
        Set<Class<?>> handlerTypes = new LinkedHashSet<>();
        Class<?> specificHandlerType = null;
        // 没被JDK动态代理
        if (!Proxy.isProxyClass(targetType)) {
            // 获得原本的类
            specificHandlerType = ClassUtils.getUserClass(targetType);
            handlerTypes.add(specificHandlerType);
        }
        // 继承的所有接口
        handlerTypes.addAll(ClassUtils.getAllInterfacesForClassAsSet(targetType));
        for (Class<?> currentHandlerType : handlerTypes) {
            final Class<?> targetClass = (specificHandlerType != null ? specificHandlerType : currentHandlerType);
            ReflectionUtils.doWithMethods(currentHandlerType,
                    // MethodCallback的实现
                    method -> {
                        // 从目标类上找到对应的方法
                        Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);
                        //
                        T result = metadataLookup.inspect(specificMethod);
                        if (result != null) {
                            Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);
                            if (bridgedMethod == specificMethod || metadataLookup.inspect(bridgedMethod) == null) {
                                //
                                methodMap.put(specificMethod, result);
                            }
                        }
                    },
                    // MethodFilter的实现：用户声明的方法，不是编译器生成的
                    ReflectionUtils.USER_DECLARED_METHODS);
        }
        return methodMap;
    }

    public static Set<Method> selectMethods(Class<?> targetType, final ReflectionUtils.MethodFilter methodFilter) {
        return selectMethods(targetType, (MetadataLookup<Boolean>) method -> (methodFilter.matches(method) ? Boolean.TRUE : null)).keySet();
    }

    public static Method selectInvocableMethod(Method method, Class<?> targetType) {
        if (method.getDeclaringClass().isAssignableFrom(targetType)) {
            return method;
        }
        try {
            String methodName = method.getName();
            Class<?>[] parameterTypes = method.getParameterTypes();
            for (Class<?> ifc : targetType.getInterfaces()) {
                try {
                    return ifc.getMethod(methodName, parameterTypes);
                } catch (NoSuchMethodException ex) {
                    // Alright, not on this interface then...
                }
            }
            // A final desperate attempt on the proxy class itself...
            return targetType.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException(String.format("Need to invoke method '%s' declared on target class '%s', " + "but not found in any interface(s) of the exposed proxy type. " + "Either pull the method up to an interface or switch to CGLIB " + "proxies by enforcing proxy-target-class mode in your configuration.", method.getName(), method.getDeclaringClass().getSimpleName()));
        }
    }

    @FunctionalInterface
    public interface MetadataLookup<T> {

        @Nullable
        T inspect(Method method);

    }

}
