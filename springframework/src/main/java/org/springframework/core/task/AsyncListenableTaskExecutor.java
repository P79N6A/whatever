package org.springframework.core.task;

import org.springframework.util.concurrent.ListenableFuture;

import java.util.concurrent.Callable;

public interface AsyncListenableTaskExecutor extends AsyncTaskExecutor {

    ListenableFuture<?> submitListenable(Runnable task);

    <T> ListenableFuture<T> submitListenable(Callable<T> task);

}
