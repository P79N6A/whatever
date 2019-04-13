package mmp;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class Executors {


    // 静态工厂方法

    /*
     * 如果是要求高吞吐量的，可以使用 SynchronousQueue
     * 如果对执行顺序有要求，可以使用 PriorityBlockingQueue
     * 如果最大积攒的待做任务有上限，可以使用 LinkedBlockingQueue
     * */

    public static ExecutorService newFixedThreadPool(int nThreads) {
        // 使用了无界队列LinkedBlockingQueue作为WorkQueue而不是有界队列ArrayBlockingQueue
        // LinkedBlockingQueue 无界 排队
        return new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    }

    public static ExecutorService newFixedThreadPool(int nThreads, ThreadFactory threadFactory) {
        return new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), threadFactory);
    }

    public static ExecutorService newSingleThreadExecutor() {
        return new FinalizableDelegatedExecutorService(new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>()));
    }

    public static ExecutorService newSingleThreadExecutor(ThreadFactory threadFactory) {
        return new FinalizableDelegatedExecutorService(new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), threadFactory));
    }

    // 无界线程池 不管多少任务提交进来，都直接运行
    public static ExecutorService newCachedThreadPool() {
        // SynchronousQueue 传递任务，并不会保存
        // corePoolSize 0
        // 自动回收
        return new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>());
    }

    public static ExecutorService newCachedThreadPool(ThreadFactory threadFactory) {
        return new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(), threadFactory);
    }


    public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize) {
        return new ScheduledThreadPoolExecutor(corePoolSize);
    }

    public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize, ThreadFactory threadFactory) {
        return new ScheduledThreadPoolExecutor(corePoolSize, threadFactory);
    }


    public static ThreadFactory defaultThreadFactory() {
        return new DefaultThreadFactory();
    }

    public static <T> Callable<T> callable(Runnable task, T result) {
        if (task == null) throw new NullPointerException();
        return new RunnableAdapter<>(task, result);
    }


    static final class RunnableAdapter<T> implements Callable<T> {
        final Runnable task;
        final T result;

        RunnableAdapter(Runnable task, T result) {
            this.task = task;
            this.result = result;
        }

        public T call() {
            task.run();
            return result;
        }
    }

    // 线程池的所有线程都由线程工厂创建
    static class DefaultThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        DefaultThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            namePrefix = "pool-" + poolNumber.getAndIncrement() + "-thread-";
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
            if (t.isDaemon()) t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY) t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }


    static class DelegatedExecutorService extends AbstractExecutorService {
        private final ExecutorService e;

        DelegatedExecutorService(ExecutorService executor) {
            e = executor;
        }

        public void execute(Runnable command) {
            e.execute(command);
        }

        public void shutdown() {
            e.shutdown();
        }

        public List<Runnable> shutdownNow() {
            return e.shutdownNow();
        }

        public boolean isShutdown() {
            return e.isShutdown();
        }

        public boolean isTerminated() {
            return e.isTerminated();
        }

        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return e.awaitTermination(timeout, unit);
        }

        public Future<?> submit(Runnable task) {
            return e.submit(task);
        }

        public <T> Future<T> submit(Callable<T> task) {
            return e.submit(task);
        }

        public <T> Future<T> submit(Runnable task, T result) {
            return e.submit(task, result);
        }

        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return e.invokeAll(tasks);
        }

        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                throws InterruptedException {
            return e.invokeAll(tasks, timeout, unit);
        }

        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
            return e.invokeAny(tasks);
        }

        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return e.invokeAny(tasks, timeout, unit);
        }
    }

    static class FinalizableDelegatedExecutorService extends DelegatedExecutorService {

        FinalizableDelegatedExecutorService(ExecutorService executor) {
            super(executor);
        }

        protected void finalize() {
            super.shutdown();
        }
    }


    private Executors() {
    }
}
