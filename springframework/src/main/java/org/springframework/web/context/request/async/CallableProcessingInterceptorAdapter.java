package org.springframework.web.context.request.async;

import org.springframework.web.context.request.NativeWebRequest;

import java.util.concurrent.Callable;

@Deprecated
public abstract class CallableProcessingInterceptorAdapter implements CallableProcessingInterceptor {

    @Override
    public <T> void beforeConcurrentHandling(NativeWebRequest request, Callable<T> task) throws Exception {
    }

    @Override
    public <T> void preProcess(NativeWebRequest request, Callable<T> task) throws Exception {
    }

    @Override
    public <T> void postProcess(NativeWebRequest request, Callable<T> task, Object concurrentResult) throws Exception {
    }

    @Override
    public <T> Object handleTimeout(NativeWebRequest request, Callable<T> task) throws Exception {
        return RESULT_NONE;
    }

    @Override
    public <T> Object handleError(NativeWebRequest request, Callable<T> task, Throwable t) throws Exception {
        return RESULT_NONE;
    }

    @Override
    public <T> void afterCompletion(NativeWebRequest request, Callable<T> task) throws Exception {
    }

}
