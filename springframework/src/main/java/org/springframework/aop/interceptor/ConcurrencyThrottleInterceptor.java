package org.springframework.aop.interceptor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.util.ConcurrencyThrottleSupport;

import java.io.Serializable;

@SuppressWarnings("serial")
public class ConcurrencyThrottleInterceptor extends ConcurrencyThrottleSupport implements MethodInterceptor, Serializable {

    public ConcurrencyThrottleInterceptor() {
        setConcurrencyLimit(1);
    }

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        beforeAccess();
        try {
            return methodInvocation.proceed();
        } finally {
            afterAccess();
        }
    }

}
