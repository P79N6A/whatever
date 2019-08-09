package org.springframework.web.context.request.async;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class DeferredResult<T> {

    private static final Object RESULT_NONE = new Object();

    private static final Log logger = LogFactory.getLog(DeferredResult.class);

    @Nullable
    private final Long timeout;

    private final Supplier<?> timeoutResult;

    private Runnable timeoutCallback;

    private Consumer<Throwable> errorCallback;

    private Runnable completionCallback;

    private DeferredResultHandler resultHandler;

    private volatile Object result = RESULT_NONE;

    private volatile boolean expired = false;

    public DeferredResult() {
        this(null, () -> RESULT_NONE);
    }

    public DeferredResult(Long timeout) {
        this(timeout, () -> RESULT_NONE);
    }

    public DeferredResult(@Nullable Long timeout, final Object timeoutResult) {
        this.timeoutResult = () -> timeoutResult;
        this.timeout = timeout;
    }

    public DeferredResult(@Nullable Long timeout, Supplier<?> timeoutResult) {
        this.timeoutResult = timeoutResult;
        this.timeout = timeout;
    }

    public final boolean isSetOrExpired() {
        return (this.result != RESULT_NONE || this.expired);
    }

    public boolean hasResult() {
        return (this.result != RESULT_NONE);
    }

    @Nullable
    public Object getResult() {
        Object resultToCheck = this.result;
        return (resultToCheck != RESULT_NONE ? resultToCheck : null);
    }

    @Nullable
    final Long getTimeoutValue() {
        return this.timeout;
    }

    public void onTimeout(Runnable callback) {
        this.timeoutCallback = callback;
    }

    public void onError(Consumer<Throwable> callback) {
        this.errorCallback = callback;
    }

    public void onCompletion(Runnable callback) {
        this.completionCallback = callback;
    }

    public final void setResultHandler(DeferredResultHandler resultHandler) {
        Assert.notNull(resultHandler, "DeferredResultHandler is required");
        // Immediate expiration check outside of the result lock
        if (this.expired) {
            return;
        }
        Object resultToHandle;
        synchronized (this) {
            // Got the lock in the meantime: double-check expiration status
            if (this.expired) {
                return;
            }
            resultToHandle = this.result;
            if (resultToHandle == RESULT_NONE) {
                // No result yet: store handler for processing once it comes in
                this.resultHandler = resultHandler;
                return;
            }
        }
        // If we get here, we need to process an existing result object immediately.
        // The decision is made within the result lock; just the handle call outside
        // of it, avoiding any deadlock potential with Servlet container locks.
        try {
            resultHandler.handleResult(resultToHandle);
        } catch (Throwable ex) {
            logger.debug("Failed to process async result", ex);
        }
    }

    public boolean setResult(T result) {
        return setResultInternal(result);
    }

    private boolean setResultInternal(Object result) {
        // Immediate expiration check outside of the result lock
        if (isSetOrExpired()) {
            return false;
        }
        DeferredResultHandler resultHandlerToUse;
        synchronized (this) {
            // Got the lock in the meantime: double-check expiration status
            if (isSetOrExpired()) {
                return false;
            }
            // At this point, we got a new result to process
            this.result = result;
            resultHandlerToUse = this.resultHandler;
            if (resultHandlerToUse == null) {
                // No result handler set yet -> let the setResultHandler implementation
                // pick up the result object and invoke the result handler for it.
                return true;
            }
            // Result handler available -> let's clear the stored reference since
            // we don't need it anymore.
            this.resultHandler = null;
        }
        // If we get here, we need to process an existing result object immediately.
        // The decision is made within the result lock; just the handle call outside
        // of it, avoiding any deadlock potential with Servlet container locks.
        resultHandlerToUse.handleResult(result);
        return true;
    }

    public boolean setErrorResult(Object result) {
        return setResultInternal(result);
    }

    final DeferredResultProcessingInterceptor getInterceptor() {
        return new DeferredResultProcessingInterceptor() {
            @Override
            public <S> boolean handleTimeout(NativeWebRequest request, DeferredResult<S> deferredResult) {
                boolean continueProcessing = true;
                try {
                    if (timeoutCallback != null) {
                        timeoutCallback.run();
                    }
                } finally {
                    Object value = timeoutResult.get();
                    if (value != RESULT_NONE) {
                        continueProcessing = false;
                        try {
                            setResultInternal(value);
                        } catch (Throwable ex) {
                            logger.debug("Failed to handle timeout result", ex);
                        }
                    }
                }
                return continueProcessing;
            }

            @Override
            public <S> boolean handleError(NativeWebRequest request, DeferredResult<S> deferredResult, Throwable t) {
                try {
                    if (errorCallback != null) {
                        errorCallback.accept(t);
                    }
                } finally {
                    try {
                        setResultInternal(t);
                    } catch (Throwable ex) {
                        logger.debug("Failed to handle error result", ex);
                    }
                }
                return false;
            }

            @Override
            public <S> void afterCompletion(NativeWebRequest request, DeferredResult<S> deferredResult) {
                expired = true;
                if (completionCallback != null) {
                    completionCallback.run();
                }
            }
        };
    }

    @FunctionalInterface
    public interface DeferredResultHandler {

        void handleResult(Object result);

    }

}
