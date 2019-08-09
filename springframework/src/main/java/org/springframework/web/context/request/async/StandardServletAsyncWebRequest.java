package org.springframework.web.context.request.async;

import org.springframework.util.Assert;
import org.springframework.web.context.request.ServletWebRequest;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class StandardServletAsyncWebRequest extends ServletWebRequest implements AsyncWebRequest, AsyncListener {

    private Long timeout;

    private AsyncContext asyncContext;

    private AtomicBoolean asyncCompleted = new AtomicBoolean(false);

    private final List<Runnable> timeoutHandlers = new ArrayList<>();

    private final List<Consumer<Throwable>> exceptionHandlers = new ArrayList<>();

    private final List<Runnable> completionHandlers = new ArrayList<>();

    public StandardServletAsyncWebRequest(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    @Override
    public void setTimeout(Long timeout) {
        Assert.state(!isAsyncStarted(), "Cannot change the timeout with concurrent handling in progress");
        this.timeout = timeout;
    }

    @Override
    public void addTimeoutHandler(Runnable timeoutHandler) {
        this.timeoutHandlers.add(timeoutHandler);
    }

    @Override
    public void addErrorHandler(Consumer<Throwable> exceptionHandler) {
        this.exceptionHandlers.add(exceptionHandler);
    }

    @Override
    public void addCompletionHandler(Runnable runnable) {
        this.completionHandlers.add(runnable);
    }

    @Override
    public boolean isAsyncStarted() {
        return (this.asyncContext != null && getRequest().isAsyncStarted());
    }

    @Override
    public boolean isAsyncComplete() {
        return this.asyncCompleted.get();
    }

    @Override
    public void startAsync() {
        Assert.state(getRequest().isAsyncSupported(), "Async support must be enabled on a servlet and for all filters involved " + "in async request processing. This is done in Java code using the Servlet API " + "or by adding \"<async-supported>true</async-supported>\" to servlet and " + "filter declarations in web.xml.");
        Assert.state(!isAsyncComplete(), "Async processing has already completed");
        if (isAsyncStarted()) {
            return;
        }
        this.asyncContext = getRequest().startAsync(getRequest(), getResponse());
        this.asyncContext.addListener(this);
        if (this.timeout != null) {
            this.asyncContext.setTimeout(this.timeout);
        }
    }

    @Override
    public void dispatch() {
        Assert.notNull(this.asyncContext, "Cannot dispatch without an AsyncContext");
        this.asyncContext.dispatch();
    }
    // ---------------------------------------------------------------------
    // Implementation of AsyncListener methods
    // ---------------------------------------------------------------------

    @Override
    public void onStartAsync(AsyncEvent event) throws IOException {
    }

    @Override
    public void onError(AsyncEvent event) throws IOException {
        this.exceptionHandlers.forEach(consumer -> consumer.accept(event.getThrowable()));
    }

    @Override
    public void onTimeout(AsyncEvent event) throws IOException {
        this.timeoutHandlers.forEach(Runnable::run);
    }

    @Override
    public void onComplete(AsyncEvent event) throws IOException {
        this.completionHandlers.forEach(Runnable::run);
        this.asyncContext = null;
        this.asyncCompleted.set(true);
    }

}
