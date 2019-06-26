package org.apache.ibatis.builder.annotation;

import org.apache.ibatis.builder.BuilderException;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public interface ProviderMethodResolver {

    default Method resolveMethod(ProviderContext context) {
        List<Method> sameNameMethods = Arrays.stream(getClass().getMethods()).filter(m -> m.getName().equals(context.getMapperMethod().getName())).collect(Collectors.toList());
        if (sameNameMethods.isEmpty()) {
            throw new BuilderException("Cannot resolve the provider method because '" + context.getMapperMethod().getName() + "' not found in SqlProvider '" + getClass().getName() + "'.");
        }
        List<Method> targetMethods = sameNameMethods.stream().filter(m -> CharSequence.class.isAssignableFrom(m.getReturnType())).collect(Collectors.toList());
        if (targetMethods.size() == 1) {
            return targetMethods.get(0);
        }
        if (targetMethods.isEmpty()) {
            throw new BuilderException("Cannot resolve the provider method because '" + context.getMapperMethod().getName() + "' does not return the CharSequence or its subclass in SqlProvider '" + getClass().getName() + "'.");
        } else {
            throw new BuilderException("Cannot resolve the provider method because '" + context.getMapperMethod().getName() + "' is found multiple in SqlProvider '" + getClass().getName() + "'.");
        }
    }

}
