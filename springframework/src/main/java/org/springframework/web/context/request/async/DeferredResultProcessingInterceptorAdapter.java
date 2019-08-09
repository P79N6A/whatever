package org.springframework.web.context.request.async;

import org.springframework.web.context.request.NativeWebRequest;

@Deprecated
public abstract class DeferredResultProcessingInterceptorAdapter implements DeferredResultProcessingInterceptor {

    @Override
    public <T> void beforeConcurrentHandling(NativeWebRequest request, DeferredResult<T> deferredResult) throws Exception {
    }

    @Override
    public <T> void preProcess(NativeWebRequest request, DeferredResult<T> deferredResult) throws Exception {
    }

    @Override
    public <T> void postProcess(NativeWebRequest request, DeferredResult<T> deferredResult, Object concurrentResult) throws Exception {
    }

    @Override
    public <T> boolean handleTimeout(NativeWebRequest request, DeferredResult<T> deferredResult) throws Exception {
        return true;
    }

    @Override
    public <T> boolean handleError(NativeWebRequest request, DeferredResult<T> deferredResult, Throwable t) throws Exception {
        return true;
    }

    @Override
    public <T> void afterCompletion(NativeWebRequest request, DeferredResult<T> deferredResult) throws Exception {
    }

}
