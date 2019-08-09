package org.springframework.aop.framework;

import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.MethodMatcher;

class InterceptorAndDynamicMethodMatcher {

    final MethodInterceptor interceptor;

    final MethodMatcher methodMatcher;

    public InterceptorAndDynamicMethodMatcher(MethodInterceptor interceptor, MethodMatcher methodMatcher) {
        this.interceptor = interceptor;
        this.methodMatcher = methodMatcher;
    }

}
