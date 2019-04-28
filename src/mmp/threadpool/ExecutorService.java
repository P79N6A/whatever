package mmp.threadpool;


import java.util.List;
import java.util.concurrent.*;

public interface ExecutorService extends Executor {

    void shutdown();

    List<Runnable> shutdownNow();


    <T> Future<T> submit(Callable<T> task);

    <T> Future<T> submit(Runnable task, T result);

    Future<?> submit(Runnable task);

}
