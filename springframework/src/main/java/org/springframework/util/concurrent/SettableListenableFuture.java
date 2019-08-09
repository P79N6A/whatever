package org.springframework.util.concurrent;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.concurrent.*;

public class SettableListenableFuture<T> implements ListenableFuture<T> {

    private static final Callable<Object> DUMMY_CALLABLE = () -> {
        throw new IllegalStateException("Should never be called");
    };

    private final SettableTask<T> settableTask = new SettableTask<>();

    public boolean set(@Nullable T value) {
        return this.settableTask.setResultValue(value);
    }

    public boolean setException(Throwable exception) {
        Assert.notNull(exception, "Exception must not be null");
        return this.settableTask.setExceptionResult(exception);
    }

    @Override
    public void addCallback(ListenableFutureCallback<? super T> callback) {
        this.settableTask.addCallback(callback);
    }

    @Override
    public void addCallback(SuccessCallback<? super T> successCallback, FailureCallback failureCallback) {
        this.settableTask.addCallback(successCallback, failureCallback);
    }

    @Override
    public CompletableFuture<T> completable() {
        return this.settableTask.completable();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean cancelled = this.settableTask.cancel(mayInterruptIfRunning);
        if (cancelled && mayInterruptIfRunning) {
            interruptTask();
        }
        return cancelled;
    }

    @Override
    public boolean isCancelled() {
        return this.settableTask.isCancelled();
    }

    @Override
    public boolean isDone() {
        return this.settableTask.isDone();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return this.settableTask.get();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return this.settableTask.get(timeout, unit);
    }

    protected void interruptTask() {
    }

    private static class SettableTask<T> extends ListenableFutureTask<T> {

        @Nullable
        private volatile Thread completingThread;

        @SuppressWarnings("unchecked")
        public SettableTask() {
            super((Callable<T>) DUMMY_CALLABLE);
        }

        public boolean setResultValue(@Nullable T value) {
            set(value);
            return checkCompletingThread();
        }

        public boolean setExceptionResult(Throwable exception) {
            setException(exception);
            return checkCompletingThread();
        }

        @Override
        protected void done() {
            if (!isCancelled()) {
                // Implicitly invoked by set/setException: store current thread for
                // determining whether the given result has actually triggered completion
                // (since FutureTask.set/setException unfortunately don't expose that)
                this.completingThread = Thread.currentThread();
            }
            super.done();
        }

        private boolean checkCompletingThread() {
            boolean check = (this.completingThread == Thread.currentThread());
            if (check) {
                this.completingThread = null;  // only first match actually counts
            }
            return check;
        }

    }

}
