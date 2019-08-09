package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

public class ResponseBodyEmitter {

    @Nullable
    private final Long timeout;

    @Nullable
    private Handler handler;

    private final Set<DataWithMediaType> earlySendAttempts = new LinkedHashSet<>(8);

    private boolean complete;

    @Nullable
    private Throwable failure;

    private boolean sendFailed;

    private final DefaultCallback timeoutCallback = new DefaultCallback();

    private final ErrorCallback errorCallback = new ErrorCallback();

    private final DefaultCallback completionCallback = new DefaultCallback();

    public ResponseBodyEmitter() {
        this.timeout = null;
    }

    public ResponseBodyEmitter(Long timeout) {
        this.timeout = timeout;
    }

    @Nullable
    public Long getTimeout() {
        return this.timeout;
    }

    synchronized void initialize(Handler handler) throws IOException {
        this.handler = handler;
        for (DataWithMediaType sendAttempt : this.earlySendAttempts) {
            sendInternal(sendAttempt.getData(), sendAttempt.getMediaType());
        }
        this.earlySendAttempts.clear();
        if (this.complete) {
            if (this.failure != null) {
                this.handler.completeWithError(this.failure);
            } else {
                this.handler.complete();
            }
        } else {
            this.handler.onTimeout(this.timeoutCallback);
            this.handler.onError(this.errorCallback);
            this.handler.onCompletion(this.completionCallback);
        }
    }

    protected void extendResponse(ServerHttpResponse outputMessage) {
    }

    public void send(Object object) throws IOException {
        send(object, null);
    }

    public synchronized void send(Object object, @Nullable MediaType mediaType) throws IOException {
        Assert.state(!this.complete, "ResponseBodyEmitter is already set complete");
        sendInternal(object, mediaType);
    }

    private void sendInternal(Object object, @Nullable MediaType mediaType) throws IOException {
        if (this.handler != null) {
            try {
                this.handler.send(object, mediaType);
            } catch (IOException ex) {
                this.sendFailed = true;
                throw ex;
            } catch (Throwable ex) {
                this.sendFailed = true;
                throw new IllegalStateException("Failed to send " + object, ex);
            }
        } else {
            this.earlySendAttempts.add(new DataWithMediaType(object, mediaType));
        }
    }

    public synchronized void complete() {
        // Ignore, after send failure
        if (this.sendFailed) {
            return;
        }
        this.complete = true;
        if (this.handler != null) {
            this.handler.complete();
        }
    }

    public synchronized void completeWithError(Throwable ex) {
        // Ignore, after send failure
        if (this.sendFailed) {
            return;
        }
        this.complete = true;
        this.failure = ex;
        if (this.handler != null) {
            this.handler.completeWithError(ex);
        }
    }

    public synchronized void onTimeout(Runnable callback) {
        this.timeoutCallback.setDelegate(callback);
    }

    public synchronized void onError(Consumer<Throwable> callback) {
        this.errorCallback.setDelegate(callback);
    }

    public synchronized void onCompletion(Runnable callback) {
        this.completionCallback.setDelegate(callback);
    }

    @Override
    public String toString() {
        return "ResponseBodyEmitter@" + ObjectUtils.getIdentityHexString(this);
    }

    interface Handler {

        void send(Object data, @Nullable MediaType mediaType) throws IOException;

        void complete();

        void completeWithError(Throwable failure);

        void onTimeout(Runnable callback);

        void onError(Consumer<Throwable> callback);

        void onCompletion(Runnable callback);

    }

    public static class DataWithMediaType {

        private final Object data;

        @Nullable
        private final MediaType mediaType;

        public DataWithMediaType(Object data, @Nullable MediaType mediaType) {
            this.data = data;
            this.mediaType = mediaType;
        }

        public Object getData() {
            return this.data;
        }

        @Nullable
        public MediaType getMediaType() {
            return this.mediaType;
        }

    }

    private class DefaultCallback implements Runnable {

        @Nullable
        private Runnable delegate;

        public void setDelegate(Runnable delegate) {
            this.delegate = delegate;
        }

        @Override
        public void run() {
            ResponseBodyEmitter.this.complete = true;
            if (this.delegate != null) {
                this.delegate.run();
            }
        }

    }

    private class ErrorCallback implements Consumer<Throwable> {

        @Nullable
        private Consumer<Throwable> delegate;

        public void setDelegate(Consumer<Throwable> callback) {
            this.delegate = callback;
        }

        @Override
        public void accept(Throwable t) {
            ResponseBodyEmitter.this.complete = true;
            if (this.delegate != null) {
                this.delegate.accept(t);
            }
        }

    }

}
