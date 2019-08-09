package org.springframework.expression;

public interface MethodExecutor {

    TypedValue execute(EvaluationContext context, Object target, Object... arguments) throws AccessException;

}
