package org.springframework.util.concurrent;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class FutureAdapter<T, S> implements Future<T> {

    private final Future<S> adaptee;

    @Nullable
    private Object result;

    private State state = State.NEW;

    private final Object mutex = new Object();

    protected FutureAdapter(Future<S> adaptee) {
        Assert.notNull(adaptee, "Delegate must not be null");
        this.adaptee = adaptee;
    }

    protected Future<S> getAdaptee() {
        return this.adaptee;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return this.adaptee.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return this.adaptee.isCancelled();
    }

    @Override
    public boolean isDone() {
        return this.adaptee.isDone();
    }

    @Override
    @Nullable
    public T get() throws InterruptedException, ExecutionException {
        return adaptInternal(this.adaptee.get());
    }

    @Override
    @Nullable
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return adaptInternal(this.adaptee.get(timeout, unit));
    }

    @SuppressWarnings("unchecked")
    @Nullable
    final T adaptInternal(S adapteeResult) throws ExecutionException {
        synchronized (this.mutex) {
            switch (this.state) {
                case SUCCESS:
                    return (T) this.result;
                case FAILURE:
                    Assert.state(this.result instanceof ExecutionException, "Failure without exception");
                    throw (ExecutionException) this.result;
                case NEW:
                    try {
                        T adapted = adapt(adapteeResult);
                        this.result = adapted;
                        this.state = State.SUCCESS;
                        return adapted;
                    } catch (ExecutionException ex) {
                        this.result = ex;
                        this.state = State.FAILURE;
                        throw ex;
                    } catch (Throwable ex) {
                        ExecutionException execEx = new ExecutionException(ex);
                        this.result = execEx;
                        this.state = State.FAILURE;
                        throw execEx;
                    }
                default:
                    throw new IllegalStateException();
            }
        }
    }

    @Nullable
    protected abstract T adapt(S adapteeResult) throws ExecutionException;

    private enum State {NEW, SUCCESS, FAILURE}

}
