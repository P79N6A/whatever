package com.alipay.remoting.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class RunStateRecordedFutureTask<V> extends FutureTask<V> {
    private AtomicBoolean hasRun = new AtomicBoolean();

    public RunStateRecordedFutureTask(Callable<V> callable) {
        super(callable);
    }

    @Override
    public void run() {
        this.hasRun.set(true);
        super.run();
    }

    public V getAfterRun() throws InterruptedException, ExecutionException, FutureTaskNotRunYetException, FutureTaskNotCompleted {
        if (!hasRun.get()) {
            throw new FutureTaskNotRunYetException();
        }
        if (!isDone()) {
            throw new FutureTaskNotCompleted();
        }
        return super.get();
    }

}