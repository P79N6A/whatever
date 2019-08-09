package org.springframework.beans.factory.support;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;

abstract class AutowireUtils {

    private static final Comparator<Executable> EXECUTABLE_COMPARATOR = (e1, e2) -> {
        int result = Boolean.compare(Modifier.isPublic(e2.getModifiers()), Modifier.isPublic(e1.getModifiers()));
        return result != 0 ? result : Integer.compare(e2.getParameterCount(), e1.getParameterCount());
    };

    public static void sortConstructors(Constructor<?>[] constructors) {
        Arrays.sort(constructors, EXECUTABLE_COMPARATOR);
    }

    public static void sortFactoryMethods(Method[] factoryMethods) {
        Arrays.sort(factoryMethods, EXECUTABLE_COMPARATOR);
    }

    public static boolean isExcludedFromDependencyCheck(PropertyDescriptor pd) {
        Method wm = pd.getWriteMethod();
        if (wm == null) {
            return false;
        }
        if (!wm.getDeclaringClass().getName().contains("$$")) {
            // Not a CGLIB method so it's OK.
            return false;
        }
        // It was declared by CGLIB, but we might still want to autowire it
        // if it was actually declared by the superclass.
        Class<?> superclass = wm.getDeclaringClass().getSuperclass();
        return !ClassUtils.hasMethod(superclass, wm.getName(), wm.getParameterTypes());
    }

    public static boolean isSetterDefinedInInterface(PropertyDescriptor pd, Set<Class<?>> interfaces) {
        Method setter = pd.getWriteMethod();
        if (setter != null) {
            Class<?> targetClass = setter.getDeclaringClass();
            for (Class<?> ifc : interfaces) {
                if (ifc.isAssignableFrom(targetClass) && ClassUtils.hasMethod(ifc, setter.getName(), setter.getParameterTypes())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static Object resolveAutowiringValue(Object autowiringValue, Class<?> requiredType) {
        if (autowiringValue instanceof ObjectFactory && !requiredType.isInstance(autowiringValue)) {
            ObjectFactory<?> factory = (ObjectFactory<?>) autowiringValue;
            if (autowiringValue instanceof Serializable && requiredType.isInterface()) {
                autowiringValue = Proxy.newProxyInstance(requiredType.getClassLoader(), new Class<?>[]{requiredType}, new ObjectFactoryDelegatingInvocationHandler(factory));
            } else {
                return factory.getObject();
            }
        }
        return autowiringValue;
    }

    public static Class<?> resolveReturnTypeForFactoryMethod(Method method, Object[] args, @Nullable ClassLoader classLoader) {
        Assert.notNull(method, "Method must not be null");
        Assert.notNull(args, "Argument array must not be null");
        TypeVariable<Method>[] declaredTypeVariables = method.getTypeParameters();
        Type genericReturnType = method.getGenericReturnType();
        Type[] methodParameterTypes = method.getGenericParameterTypes();
        Assert.isTrue(args.length == methodParameterTypes.length, "Argument array does not match parameter count");
        // Ensure that the type variable (e.g., T) is declared directly on the method
        // itself (e.g., via <T>), not on the enclosing class or interface.
        boolean locallyDeclaredTypeVariableMatchesReturnType = false;
        for (TypeVariable<Method> currentTypeVariable : declaredTypeVariables) {
            if (currentTypeVariable.equals(genericReturnType)) {
                locallyDeclaredTypeVariableMatchesReturnType = true;
                break;
            }
        }
        if (locallyDeclaredTypeVariableMatchesReturnType) {
            for (int i = 0; i < methodParameterTypes.length; i++) {
                Type methodParameterType = methodParameterTypes[i];
                Object arg = args[i];
                if (methodParameterType.equals(genericReturnType)) {
                    if (arg instanceof TypedStringValue) {
                        TypedStringValue typedValue = ((TypedStringValue) arg);
                        if (typedValue.hasTargetType()) {
                            return typedValue.getTargetType();
                        }
                        try {
                            Class<?> resolvedType = typedValue.resolveTargetType(classLoader);
                            if (resolvedType != null) {
                                return resolvedType;
                            }
                        } catch (ClassNotFoundException ex) {
                            throw new IllegalStateException("Failed to resolve value type [" + typedValue.getTargetTypeName() + "] for factory method argument", ex);
                        }
                    } else if (arg != null && !(arg instanceof BeanMetadataElement)) {
                        // Only consider argument type if it is a simple value...
                        return arg.getClass();
                    }
                    return method.getReturnType();
                } else if (methodParameterType instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) methodParameterType;
                    Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                    for (Type typeArg : actualTypeArguments) {
                        if (typeArg.equals(genericReturnType)) {
                            if (arg instanceof Class) {
                                return (Class<?>) arg;
                            } else {
                                String className = null;
                                if (arg instanceof String) {
                                    className = (String) arg;
                                } else if (arg instanceof TypedStringValue) {
                                    TypedStringValue typedValue = ((TypedStringValue) arg);
                                    String targetTypeName = typedValue.getTargetTypeName();
                                    if (targetTypeName == null || Class.class.getName().equals(targetTypeName)) {
                                        className = typedValue.getValue();
                                    }
                                }
                                if (className != null) {
                                    try {
                                        return ClassUtils.forName(className, classLoader);
                                    } catch (ClassNotFoundException ex) {
                                        throw new IllegalStateException("Could not resolve class name [" + arg + "] for factory method argument", ex);
                                    }
                                }
                                // Consider adding logic to determine the class of the typeArg, if possible.
                                // For now, just fall back...
                                return method.getReturnType();
                            }
                        }
                    }
                }
            }
        }
        // Fall back...
        return method.getReturnType();
    }

    @SuppressWarnings("serial")
    private static class ObjectFactoryDelegatingInvocationHandler implements InvocationHandler, Serializable {

        private final ObjectFactory<?> objectFactory;

        public ObjectFactoryDelegatingInvocationHandler(ObjectFactory<?> objectFactory) {
            this.objectFactory = objectFactory;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            if (methodName.equals("equals")) {
                // Only consider equal when proxies are identical.
                return (proxy == args[0]);
            } else if (methodName.equals("hashCode")) {
                // Use hashCode of proxy.
                return System.identityHashCode(proxy);
            } else if (methodName.equals("toString")) {
                return this.objectFactory.toString();
            }
            try {
                return method.invoke(this.objectFactory.getObject(), args);
            } catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            }
        }

    }

}
