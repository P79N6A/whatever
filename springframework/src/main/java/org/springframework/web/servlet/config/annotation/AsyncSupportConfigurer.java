package org.springframework.web.servlet.config.annotation;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.lang.Nullable;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.context.request.async.DeferredResultProcessingInterceptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AsyncSupportConfigurer {

    @Nullable
    private AsyncTaskExecutor taskExecutor;

    @Nullable
    private Long timeout;

    private final List<CallableProcessingInterceptor> callableInterceptors = new ArrayList<>();

    private final List<DeferredResultProcessingInterceptor> deferredResultInterceptors = new ArrayList<>();

    public AsyncSupportConfigurer setTaskExecutor(AsyncTaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
        return this;
    }

    public AsyncSupportConfigurer setDefaultTimeout(long timeout) {
        this.timeout = timeout;
        return this;
    }

    public AsyncSupportConfigurer registerCallableInterceptors(CallableProcessingInterceptor... interceptors) {
        this.callableInterceptors.addAll(Arrays.asList(interceptors));
        return this;
    }

    public AsyncSupportConfigurer registerDeferredResultInterceptors(DeferredResultProcessingInterceptor... interceptors) {
        this.deferredResultInterceptors.addAll(Arrays.asList(interceptors));
        return this;
    }

    @Nullable
    protected AsyncTaskExecutor getTaskExecutor() {
        return this.taskExecutor;
    }

    @Nullable
    protected Long getTimeout() {
        return this.timeout;
    }

    protected List<CallableProcessingInterceptor> getCallableInterceptors() {
        return this.callableInterceptors;
    }

    protected List<DeferredResultProcessingInterceptor> getDeferredResultInterceptors() {
        return this.deferredResultInterceptors;
    }

}
