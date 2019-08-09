package org.springframework.expression;

import org.springframework.lang.Nullable;

public interface OperatorOverloader {

    boolean overridesOperation(Operation operation, @Nullable Object leftOperand, @Nullable Object rightOperand) throws EvaluationException;

    Object operate(Operation operation, @Nullable Object leftOperand, @Nullable Object rightOperand) throws EvaluationException;

}
