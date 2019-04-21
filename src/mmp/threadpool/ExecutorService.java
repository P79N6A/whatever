package mmp.threadpool;

import mmp.Callable;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

public interface ExecutorService extends Executor {

    void shutdown();

    List<Runnable> shutdownNow();

    boolean isShutdown();

    boolean isTerminated();

    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;

    <T> mmp.Future<T> submit(mmp.Callable<T> task);

    <T> mmp.Future<T> submit(Runnable task, T result);

    mmp.Future<?> submit(Runnable task);

    <T> List<mmp.Future<T>> invokeAll(Collection<? extends mmp.Callable<T>> tasks) throws InterruptedException;

    <T> List<mmp.Future<T>> invokeAll(Collection<? extends mmp.Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException;

    <T> T invokeAny(Collection<? extends mmp.Callable<T>> tasks) throws InterruptedException, ExecutionException;

    <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;
}
