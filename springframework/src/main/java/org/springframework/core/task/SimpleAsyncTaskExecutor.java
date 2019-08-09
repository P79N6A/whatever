package org.springframework.core.task;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrencyThrottleSupport;
import org.springframework.util.CustomizableThreadCreator;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureTask;

import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;

@SuppressWarnings("serial")
public class SimpleAsyncTaskExecutor extends CustomizableThreadCreator implements AsyncListenableTaskExecutor, Serializable {

    public static final int UNBOUNDED_CONCURRENCY = ConcurrencyThrottleSupport.UNBOUNDED_CONCURRENCY;

    public static final int NO_CONCURRENCY = ConcurrencyThrottleSupport.NO_CONCURRENCY;

    private final ConcurrencyThrottleAdapter concurrencyThrottle = new ConcurrencyThrottleAdapter();

    @Nullable
    private ThreadFactory threadFactory;

    @Nullable
    private TaskDecorator taskDecorator;

    public SimpleAsyncTaskExecutor() {
        super();
    }

    public SimpleAsyncTaskExecutor(String threadNamePrefix) {
        super(threadNamePrefix);
    }

    public SimpleAsyncTaskExecutor(ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
    }

    public void setThreadFactory(@Nullable ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
    }

    @Nullable
    public final ThreadFactory getThreadFactory() {
        return this.threadFactory;
    }

    public final void setTaskDecorator(TaskDecorator taskDecorator) {
        this.taskDecorator = taskDecorator;
    }

    public void setConcurrencyLimit(int concurrencyLimit) {
        this.concurrencyThrottle.setConcurrencyLimit(concurrencyLimit);
    }

    public final int getConcurrencyLimit() {
        return this.concurrencyThrottle.getConcurrencyLimit();
    }

    public final boolean isThrottleActive() {
        return this.concurrencyThrottle.isThrottleActive();
    }

    @Override
    public void execute(Runnable task) {
        execute(task, TIMEOUT_INDEFINITE);
    }

    @Override
    public void execute(Runnable task, long startTimeout) {
        Assert.notNull(task, "Runnable must not be null");
        Runnable taskToUse = (this.taskDecorator != null ? this.taskDecorator.decorate(task) : task);
        if (isThrottleActive() && startTimeout > TIMEOUT_IMMEDIATE) {
            this.concurrencyThrottle.beforeAccess();
            doExecute(new ConcurrencyThrottlingRunnable(taskToUse));
        } else {
            doExecute(taskToUse);
        }
    }

    @Override
    public Future<?> submit(Runnable task) {
        FutureTask<Object> future = new FutureTask<>(task, null);
        execute(future, TIMEOUT_INDEFINITE);
        return future;
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        FutureTask<T> future = new FutureTask<>(task);
        execute(future, TIMEOUT_INDEFINITE);
        return future;
    }

    @Override
    public ListenableFuture<?> submitListenable(Runnable task) {
        ListenableFutureTask<Object> future = new ListenableFutureTask<>(task, null);
        execute(future, TIMEOUT_INDEFINITE);
        return future;
    }

    @Override
    public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
        ListenableFutureTask<T> future = new ListenableFutureTask<>(task);
        execute(future, TIMEOUT_INDEFINITE);
        return future;
    }

    protected void doExecute(Runnable task) {
        Thread thread = (this.threadFactory != null ? this.threadFactory.newThread(task) : createThread(task));
        thread.start();
    }

    private static class ConcurrencyThrottleAdapter extends ConcurrencyThrottleSupport {

        @Override
        protected void beforeAccess() {
            super.beforeAccess();
        }

        @Override
        protected void afterAccess() {
            super.afterAccess();
        }

    }

    private class ConcurrencyThrottlingRunnable implements Runnable {

        private final Runnable target;

        public ConcurrencyThrottlingRunnable(Runnable target) {
            this.target = target;
        }

        @Override
        public void run() {
            try {
                this.target.run();
            } finally {
                concurrencyThrottle.afterAccess();
            }
        }

    }

}
