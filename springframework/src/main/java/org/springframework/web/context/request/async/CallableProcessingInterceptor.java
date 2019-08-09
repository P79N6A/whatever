package org.springframework.web.context.request.async;

import org.springframework.web.context.request.NativeWebRequest;

import java.util.concurrent.Callable;

public interface CallableProcessingInterceptor {

    Object RESULT_NONE = new Object();

    Object RESPONSE_HANDLED = new Object();

    default <T> void beforeConcurrentHandling(NativeWebRequest request, Callable<T> task) throws Exception {
    }

    default <T> void preProcess(NativeWebRequest request, Callable<T> task) throws Exception {
    }

    default <T> void postProcess(NativeWebRequest request, Callable<T> task, Object concurrentResult) throws Exception {
    }

    default <T> Object handleTimeout(NativeWebRequest request, Callable<T> task) throws Exception {
        return RESULT_NONE;
    }

    default <T> Object handleError(NativeWebRequest request, Callable<T> task, Throwable t) throws Exception {
        return RESULT_NONE;
    }

    default <T> void afterCompletion(NativeWebRequest request, Callable<T> task) throws Exception {
    }

}
