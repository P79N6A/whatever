package org.springframework.scheduling.annotation;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.lang.Nullable;

import java.util.concurrent.Executor;

public class AsyncConfigurerSupport implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        return null;
    }

    @Override
    @Nullable
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return null;
    }

}
