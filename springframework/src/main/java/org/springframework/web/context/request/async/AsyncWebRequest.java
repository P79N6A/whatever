package org.springframework.web.context.request.async;

import org.springframework.lang.Nullable;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.function.Consumer;

public interface AsyncWebRequest extends NativeWebRequest {

    void setTimeout(@Nullable Long timeout);

    void addTimeoutHandler(Runnable runnable);

    void addErrorHandler(Consumer<Throwable> exceptionHandler);

    void addCompletionHandler(Runnable runnable);

    void startAsync();

    boolean isAsyncStarted();

    void dispatch();

    boolean isAsyncComplete();

}
