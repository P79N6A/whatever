package org.springframework.web.context.request.async;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.concurrent.Callable;

public class WebAsyncTask<V> implements BeanFactoryAware {

    private final Callable<V> callable;

    private Long timeout;

    private AsyncTaskExecutor executor;

    private String executorName;

    private BeanFactory beanFactory;

    private Callable<V> timeoutCallback;

    private Callable<V> errorCallback;

    private Runnable completionCallback;

    public WebAsyncTask(Callable<V> callable) {
        Assert.notNull(callable, "Callable must not be null");
        this.callable = callable;
    }

    public WebAsyncTask(long timeout, Callable<V> callable) {
        this(callable);
        this.timeout = timeout;
    }

    public WebAsyncTask(@Nullable Long timeout, String executorName, Callable<V> callable) {
        this(callable);
        Assert.notNull(executorName, "Executor name must not be null");
        this.executorName = executorName;
        this.timeout = timeout;
    }

    public WebAsyncTask(@Nullable Long timeout, AsyncTaskExecutor executor, Callable<V> callable) {
        this(callable);
        Assert.notNull(executor, "Executor must not be null");
        this.executor = executor;
        this.timeout = timeout;
    }

    public Callable<?> getCallable() {
        return this.callable;
    }

    @Nullable
    public Long getTimeout() {
        return this.timeout;
    }

    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Nullable
    public AsyncTaskExecutor getExecutor() {
        if (this.executor != null) {
            return this.executor;
        } else if (this.executorName != null) {
            Assert.state(this.beanFactory != null, "BeanFactory is required to look up an executor bean by name");
            return this.beanFactory.getBean(this.executorName, AsyncTaskExecutor.class);
        } else {
            return null;
        }
    }

    public void onTimeout(Callable<V> callback) {
        this.timeoutCallback = callback;
    }

    public void onError(Callable<V> callback) {
        this.errorCallback = callback;
    }

    public void onCompletion(Runnable callback) {
        this.completionCallback = callback;
    }

    CallableProcessingInterceptor getInterceptor() {
        return new CallableProcessingInterceptor() {
            @Override
            public <T> Object handleTimeout(NativeWebRequest request, Callable<T> task) throws Exception {
                return (timeoutCallback != null ? timeoutCallback.call() : CallableProcessingInterceptor.RESULT_NONE);
            }

            @Override
            public <T> Object handleError(NativeWebRequest request, Callable<T> task, Throwable t) throws Exception {
                return (errorCallback != null ? errorCallback.call() : CallableProcessingInterceptor.RESULT_NONE);
            }

            @Override
            public <T> void afterCompletion(NativeWebRequest request, Callable<T> task) throws Exception {
                if (completionCallback != null) {
                    completionCallback.run();
                }
            }
        };
    }

}
