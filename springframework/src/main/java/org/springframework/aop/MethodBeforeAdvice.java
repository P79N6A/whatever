package org.springframework.aop;

import org.springframework.lang.Nullable;

import java.lang.reflect.Method;

public interface MethodBeforeAdvice extends BeforeAdvice {

    void before(Method method, Object[] args, @Nullable Object target) throws Throwable;

}
