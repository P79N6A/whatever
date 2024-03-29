package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;

public class DeferredResultMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

    @Override
    public boolean supportsReturnType(MethodParameter returnType) {
        Class<?> type = returnType.getParameterType();
        return (DeferredResult.class.isAssignableFrom(type) || ListenableFuture.class.isAssignableFrom(type) || CompletionStage.class.isAssignableFrom(type));
    }

    @Override
    public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType, ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {
        if (returnValue == null) {
            mavContainer.setRequestHandled(true);
            return;
        }
        DeferredResult<?> result;
        if (returnValue instanceof DeferredResult) {
            result = (DeferredResult<?>) returnValue;
        } else if (returnValue instanceof ListenableFuture) {
            result = adaptListenableFuture((ListenableFuture<?>) returnValue);
        } else if (returnValue instanceof CompletionStage) {
            result = adaptCompletionStage((CompletionStage<?>) returnValue);
        } else {
            // Should not happen...
            throw new IllegalStateException("Unexpected return value type: " + returnValue);
        }
        WebAsyncUtils.getAsyncManager(webRequest).startDeferredResultProcessing(result, mavContainer);
    }

    private DeferredResult<Object> adaptListenableFuture(ListenableFuture<?> future) {
        DeferredResult<Object> result = new DeferredResult<>();
        future.addCallback(new ListenableFutureCallback<Object>() {
            @Override
            public void onSuccess(@Nullable Object value) {
                result.setResult(value);
            }

            @Override
            public void onFailure(Throwable ex) {
                result.setErrorResult(ex);
            }
        });
        return result;
    }

    private DeferredResult<Object> adaptCompletionStage(CompletionStage<?> future) {
        DeferredResult<Object> result = new DeferredResult<>();
        future.handle((BiFunction<Object, Throwable, Object>) (value, ex) -> {
            if (ex != null) {
                if (ex instanceof CompletionException && ex.getCause() != null) {
                    ex = ex.getCause();
                }
                result.setErrorResult(ex);
            } else {
                result.setResult(value);
            }
            return null;
        });
        return result;
    }

}
