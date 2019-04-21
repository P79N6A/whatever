package mmp.threadpool;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public abstract class AbstractExecutorService implements ExecutorService {

    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new FutureTask<>(runnable, value);
    }

    protected <T> RunnableFuture<T> newTaskFor(mmp.Callable<T> callable) {
        return new FutureTask<>(callable);
    }

    // 向线程池提交任务的时候，线程池会创建一个 FutureTask 返回
    public mmp.Future<?> submit(Runnable task) {
        if (task == null) throw new NullPointerException();
        RunnableFuture<Void> ftask = newTaskFor(task, null);
        execute(ftask);
        return ftask;
    }

    // 提交一个 Runnable 任务，返回一个 Future
    public <T> mmp.Future<T> submit(Runnable task, T result) {
        if (task == null) throw new NullPointerException();
        RunnableFuture<T> ftask = newTaskFor(task, result);
        execute(ftask);
        return ftask;
    }

    //  提交一个 Callable 任务，返回一个 Future
    public <T> mmp.Future<T> submit(mmp.Callable<T> task) {
        if (task == null) throw new NullPointerException();
        RunnableFuture<T> ftask = newTaskFor(task);
        execute(ftask);
        return ftask;
    }


    private <T> T doInvokeAny(Collection<? extends mmp.Callable<T>> tasks, boolean timed, long nanos) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }

    public <T> T invokeAny(Collection<? extends mmp.Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return null;
    }

    public <T> T invokeAny(Collection<? extends mmp.Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }

    public <T> List<mmp.Future<T>> invokeAll(Collection<? extends mmp.Callable<T>> tasks) throws InterruptedException {
        return null;
    }

    public <T> List<mmp.Future<T>> invokeAll(Collection<? extends mmp.Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return null;
    }

}
