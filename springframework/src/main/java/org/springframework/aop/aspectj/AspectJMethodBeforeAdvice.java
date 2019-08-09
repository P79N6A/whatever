package org.springframework.aop.aspectj;

import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.lang.Nullable;

import java.io.Serializable;
import java.lang.reflect.Method;

@SuppressWarnings("serial")
public class AspectJMethodBeforeAdvice extends AbstractAspectJAdvice implements MethodBeforeAdvice, Serializable {

    public AspectJMethodBeforeAdvice(Method aspectJBeforeAdviceMethod, AspectJExpressionPointcut pointcut, AspectInstanceFactory aif) {
        super(aspectJBeforeAdviceMethod, pointcut, aif);
    }

    @Override
    public void before(Method method, Object[] args, @Nullable Object target) throws Throwable {
        invokeAdviceMethod(getJoinPointMatch(), null, null);
    }

    @Override
    public boolean isBeforeAdvice() {
        return true;
    }

    @Override
    public boolean isAfterAdvice() {
        return false;
    }

}
