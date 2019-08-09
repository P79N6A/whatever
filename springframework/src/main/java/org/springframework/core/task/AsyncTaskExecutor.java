package org.springframework.core.task;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public interface AsyncTaskExecutor extends TaskExecutor {

    long TIMEOUT_IMMEDIATE = 0;

    long TIMEOUT_INDEFINITE = Long.MAX_VALUE;

    void execute(Runnable task, long startTimeout);

    Future<?> submit(Runnable task);

    <T> Future<T> submit(Callable<T> task);

}
