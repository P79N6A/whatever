package org.springframework.expression.spel.support;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.lang.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

public final class DataBindingMethodResolver extends ReflectiveMethodResolver {

    private DataBindingMethodResolver() {
        super();
    }

    @Override
    @Nullable
    public MethodExecutor resolve(EvaluationContext context, Object targetObject, String name, List<TypeDescriptor> argumentTypes) throws AccessException {
        if (targetObject instanceof Class) {
            throw new IllegalArgumentException("DataBindingMethodResolver does not support Class targets");
        }
        return super.resolve(context, targetObject, name, argumentTypes);
    }

    @Override
    protected boolean isCandidateForInvocation(Method method, Class<?> targetClass) {
        if (Modifier.isStatic(method.getModifiers())) {
            return false;
        }
        Class<?> clazz = method.getDeclaringClass();
        return (clazz != Object.class && clazz != Class.class && !ClassLoader.class.isAssignableFrom(targetClass));
    }

    public static DataBindingMethodResolver forInstanceMethodInvocation() {
        return new DataBindingMethodResolver();
    }

}
