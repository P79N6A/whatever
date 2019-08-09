package org.springframework.core;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;

import java.lang.reflect.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class GenericTypeResolver {

    @SuppressWarnings("rawtypes")
    private static final Map<Class<?>, Map<TypeVariable, Type>> typeVariableCache = new ConcurrentReferenceHashMap<>();

    private GenericTypeResolver() {
    }

    public static Class<?> resolveParameterType(MethodParameter methodParameter, Class<?> implementationClass) {
        Assert.notNull(methodParameter, "MethodParameter must not be null");
        Assert.notNull(implementationClass, "Class must not be null");
        methodParameter.setContainingClass(implementationClass);
        ResolvableType.resolveMethodParameter(methodParameter);
        return methodParameter.getParameterType();
    }

    public static Class<?> resolveReturnType(Method method, Class<?> clazz) {
        Assert.notNull(method, "Method must not be null");
        Assert.notNull(clazz, "Class must not be null");
        return ResolvableType.forMethodReturnType(method, clazz).resolve(method.getReturnType());
    }

    @Nullable
    public static Class<?> resolveReturnTypeArgument(Method method, Class<?> genericIfc) {
        Assert.notNull(method, "Method must not be null");
        ResolvableType resolvableType = ResolvableType.forMethodReturnType(method).as(genericIfc);
        if (!resolvableType.hasGenerics() || resolvableType.getType() instanceof WildcardType) {
            return null;
        }
        return getSingleGeneric(resolvableType);
    }

    @Nullable
    public static Class<?> resolveTypeArgument(Class<?> clazz, Class<?> genericIfc) {
        ResolvableType resolvableType = ResolvableType.forClass(clazz).as(genericIfc);
        if (!resolvableType.hasGenerics()) {
            return null;
        }
        return getSingleGeneric(resolvableType);
    }

    @Nullable
    private static Class<?> getSingleGeneric(ResolvableType resolvableType) {
        Assert.isTrue(resolvableType.getGenerics().length == 1, () -> "Expected 1 type argument on generic interface [" + resolvableType + "] but found " + resolvableType.getGenerics().length);
        return resolvableType.getGeneric().resolve();
    }

    @Nullable
    public static Class<?>[] resolveTypeArguments(Class<?> clazz, Class<?> genericIfc) {
        ResolvableType type = ResolvableType.forClass(clazz).as(genericIfc);
        if (!type.hasGenerics() || type.isEntirelyUnresolvable()) {
            return null;
        }
        return type.resolveGenerics(Object.class);
    }

    public static Type resolveType(Type genericType, @Nullable Class<?> contextClass) {
        if (contextClass != null) {
            if (genericType instanceof TypeVariable) {
                ResolvableType resolvedTypeVariable = resolveVariable((TypeVariable<?>) genericType, ResolvableType.forClass(contextClass));
                if (resolvedTypeVariable != ResolvableType.NONE) {
                    Class<?> resolved = resolvedTypeVariable.resolve();
                    if (resolved != null) {
                        return resolved;
                    }
                }
            } else if (genericType instanceof ParameterizedType) {
                ResolvableType resolvedType = ResolvableType.forType(genericType);
                if (resolvedType.hasUnresolvableGenerics()) {
                    ParameterizedType parameterizedType = (ParameterizedType) genericType;
                    Class<?>[] generics = new Class<?>[parameterizedType.getActualTypeArguments().length];
                    Type[] typeArguments = parameterizedType.getActualTypeArguments();
                    for (int i = 0; i < typeArguments.length; i++) {
                        Type typeArgument = typeArguments[i];
                        if (typeArgument instanceof TypeVariable) {
                            ResolvableType resolvedTypeArgument = resolveVariable((TypeVariable<?>) typeArgument, ResolvableType.forClass(contextClass));
                            if (resolvedTypeArgument != ResolvableType.NONE) {
                                generics[i] = resolvedTypeArgument.resolve();
                            } else {
                                generics[i] = ResolvableType.forType(typeArgument).resolve();
                            }
                        } else {
                            generics[i] = ResolvableType.forType(typeArgument).resolve();
                        }
                    }
                    Class<?> rawClass = resolvedType.getRawClass();
                    if (rawClass != null) {
                        return ResolvableType.forClassWithGenerics(rawClass, generics).getType();
                    }
                }
            }
        }
        return genericType;
    }

    private static ResolvableType resolveVariable(TypeVariable<?> typeVariable, ResolvableType contextType) {
        ResolvableType resolvedType;
        if (contextType.hasGenerics()) {
            resolvedType = ResolvableType.forType(typeVariable, contextType);
            if (resolvedType.resolve() != null) {
                return resolvedType;
            }
        }
        ResolvableType superType = contextType.getSuperType();
        if (superType != ResolvableType.NONE) {
            resolvedType = resolveVariable(typeVariable, superType);
            if (resolvedType.resolve() != null) {
                return resolvedType;
            }
        }
        for (ResolvableType ifc : contextType.getInterfaces()) {
            resolvedType = resolveVariable(typeVariable, ifc);
            if (resolvedType.resolve() != null) {
                return resolvedType;
            }
        }
        return ResolvableType.NONE;
    }

    @SuppressWarnings("rawtypes")
    public static Class<?> resolveType(Type genericType, Map<TypeVariable, Type> map) {
        return ResolvableType.forType(genericType, new TypeVariableMapVariableResolver(map)).toClass();
    }

    @SuppressWarnings("rawtypes")
    public static Map<TypeVariable, Type> getTypeVariableMap(Class<?> clazz) {
        Map<TypeVariable, Type> typeVariableMap = typeVariableCache.get(clazz);
        if (typeVariableMap == null) {
            typeVariableMap = new HashMap<>();
            buildTypeVariableMap(ResolvableType.forClass(clazz), typeVariableMap);
            typeVariableCache.put(clazz, Collections.unmodifiableMap(typeVariableMap));
        }
        return typeVariableMap;
    }

    @SuppressWarnings("rawtypes")
    private static void buildTypeVariableMap(ResolvableType type, Map<TypeVariable, Type> typeVariableMap) {
        if (type != ResolvableType.NONE) {
            Class<?> resolved = type.resolve();
            if (resolved != null && type.getType() instanceof ParameterizedType) {
                TypeVariable<?>[] variables = resolved.getTypeParameters();
                for (int i = 0; i < variables.length; i++) {
                    ResolvableType generic = type.getGeneric(i);
                    while (generic.getType() instanceof TypeVariable<?>) {
                        generic = generic.resolveType();
                    }
                    if (generic != ResolvableType.NONE) {
                        typeVariableMap.put(variables[i], generic.getType());
                    }
                }
            }
            buildTypeVariableMap(type.getSuperType(), typeVariableMap);
            for (ResolvableType interfaceType : type.getInterfaces()) {
                buildTypeVariableMap(interfaceType, typeVariableMap);
            }
            if (resolved != null && resolved.isMemberClass()) {
                buildTypeVariableMap(ResolvableType.forClass(resolved.getEnclosingClass()), typeVariableMap);
            }
        }
    }

    @SuppressWarnings({"serial", "rawtypes"})
    private static class TypeVariableMapVariableResolver implements ResolvableType.VariableResolver {

        private final Map<TypeVariable, Type> typeVariableMap;

        public TypeVariableMapVariableResolver(Map<TypeVariable, Type> typeVariableMap) {
            this.typeVariableMap = typeVariableMap;
        }

        @Override
        @Nullable
        public ResolvableType resolveVariable(TypeVariable<?> variable) {
            Type type = this.typeVariableMap.get(variable);
            return (type != null ? ResolvableType.forType(type) : null);
        }

        @Override
        public Object getSource() {
            return this.typeVariableMap;
        }

    }

}
