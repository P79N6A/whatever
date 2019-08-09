package org.springframework.expression;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;

import java.util.List;

@FunctionalInterface
public interface ConstructorResolver {

    @Nullable
    ConstructorExecutor resolve(EvaluationContext context, String typeName, List<TypeDescriptor> argumentTypes) throws AccessException;

}
