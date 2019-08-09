package org.springframework.scheduling.annotation;

import org.springframework.aop.interceptor.AsyncExecutionInterceptor;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

public class AnnotationAsyncExecutionInterceptor extends AsyncExecutionInterceptor {

    public AnnotationAsyncExecutionInterceptor(@Nullable Executor defaultExecutor) {
        super(defaultExecutor);
    }

    public AnnotationAsyncExecutionInterceptor(@Nullable Executor defaultExecutor, AsyncUncaughtExceptionHandler exceptionHandler) {
        super(defaultExecutor, exceptionHandler);
    }

    @Override
    @Nullable
    protected String getExecutorQualifier(Method method) {
        // Maintainer's note: changes made here should also be made in
        // AnnotationAsyncExecutionAspect#getExecutorQualifier
        Async async = AnnotatedElementUtils.findMergedAnnotation(method, Async.class);
        if (async == null) {
            async = AnnotatedElementUtils.findMergedAnnotation(method.getDeclaringClass(), Async.class);
        }
        return (async != null ? async.value() : null);
    }

}
