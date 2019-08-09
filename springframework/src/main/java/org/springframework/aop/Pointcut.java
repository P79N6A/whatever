package org.springframework.aop;

public interface Pointcut {

    ClassFilter getClassFilter();

    MethodMatcher getMethodMatcher();

    Pointcut TRUE = TruePointcut.INSTANCE;

}
