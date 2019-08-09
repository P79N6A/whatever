package org.springframework.aop.support;

import org.springframework.aop.Pointcut;
import org.springframework.lang.Nullable;

public interface ExpressionPointcut extends Pointcut {

    @Nullable
    String getExpression();

}
