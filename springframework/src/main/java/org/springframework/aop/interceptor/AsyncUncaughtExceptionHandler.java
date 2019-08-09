package org.springframework.aop.interceptor;

import java.lang.reflect.Method;

@FunctionalInterface
public interface AsyncUncaughtExceptionHandler {

    void handleUncaughtException(Throwable ex, Method method, Object... params);

}
