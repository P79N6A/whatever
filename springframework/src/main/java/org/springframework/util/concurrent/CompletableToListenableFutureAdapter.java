package org.springframework.util.concurrent;

import java.util.concurrent.*;

public class CompletableToListenableFutureAdapter<T> implements ListenableFuture<T> {

    private final CompletableFuture<T> completableFuture;

    private final ListenableFutureCallbackRegistry<T> callbacks = new ListenableFutureCallbackRegistry<>();

    public CompletableToListenableFutureAdapter(CompletionStage<T> completionStage) {
        this(completionStage.toCompletableFuture());
    }

    public CompletableToListenableFutureAdapter(CompletableFuture<T> completableFuture) {
        this.completableFuture = completableFuture;
        this.completableFuture.whenComplete((result, ex) -> {
            if (ex != null) {
                this.callbacks.failure(ex);
            } else {
                this.callbacks.success(result);
            }
        });
    }

    @Override
    public void addCallback(ListenableFutureCallback<? super T> callback) {
        this.callbacks.addCallback(callback);
    }

    @Override
    public void addCallback(SuccessCallback<? super T> successCallback, FailureCallback failureCallback) {
        this.callbacks.addSuccessCallback(successCallback);
        this.callbacks.addFailureCallback(failureCallback);
    }

    @Override
    public CompletableFuture<T> completable() {
        return this.completableFuture;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return this.completableFuture.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return this.completableFuture.isCancelled();
    }

    @Override
    public boolean isDone() {
        return this.completableFuture.isDone();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return this.completableFuture.get();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return this.completableFuture.get(timeout, unit);
    }

}
