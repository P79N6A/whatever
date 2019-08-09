package org.springframework.expression;
// TODO Is the resolver/executor model too pervasive in this package?

public interface ConstructorExecutor {

    TypedValue execute(EvaluationContext context, Object... arguments) throws AccessException;

}
