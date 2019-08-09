package org.springframework.expression;

import org.springframework.lang.Nullable;

public interface PropertyAccessor {

    @Nullable
    Class<?>[] getSpecificTargetClasses();

    boolean canRead(EvaluationContext context, @Nullable Object target, String name) throws AccessException;

    TypedValue read(EvaluationContext context, @Nullable Object target, String name) throws AccessException;

    boolean canWrite(EvaluationContext context, @Nullable Object target, String name) throws AccessException;

    void write(EvaluationContext context, @Nullable Object target, String name, @Nullable Object newValue) throws AccessException;

}
