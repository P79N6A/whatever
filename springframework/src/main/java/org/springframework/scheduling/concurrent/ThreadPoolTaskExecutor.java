package org.springframework.scheduling.concurrent;

import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureTask;

import java.util.Map;
import java.util.concurrent.*;

@SuppressWarnings("serial")
public class ThreadPoolTaskExecutor extends ExecutorConfigurationSupport implements AsyncListenableTaskExecutor, SchedulingTaskExecutor {

    private final Object poolSizeMonitor = new Object();

    private int corePoolSize = 1;

    private int maxPoolSize = Integer.MAX_VALUE;

    private int keepAliveSeconds = 60;

    private int queueCapacity = Integer.MAX_VALUE;

    private boolean allowCoreThreadTimeOut = false;

    @Nullable
    private TaskDecorator taskDecorator;

    @Nullable
    private ThreadPoolExecutor threadPoolExecutor;

    // Runnable decorator to user-level FutureTask, if different
    private final Map<Runnable, Object> decoratedTaskMap = new ConcurrentReferenceHashMap<>(16, ConcurrentReferenceHashMap.ReferenceType.WEAK);

    public void setCorePoolSize(int corePoolSize) {
        synchronized (this.poolSizeMonitor) {
            this.corePoolSize = corePoolSize;
            if (this.threadPoolExecutor != null) {
                this.threadPoolExecutor.setCorePoolSize(corePoolSize);
            }
        }
    }

    public int getCorePoolSize() {
        synchronized (this.poolSizeMonitor) {
            return this.corePoolSize;
        }
    }

    public void setMaxPoolSize(int maxPoolSize) {
        synchronized (this.poolSizeMonitor) {
            this.maxPoolSize = maxPoolSize;
            if (this.threadPoolExecutor != null) {
                this.threadPoolExecutor.setMaximumPoolSize(maxPoolSize);
            }
        }
    }

    public int getMaxPoolSize() {
        synchronized (this.poolSizeMonitor) {
            return this.maxPoolSize;
        }
    }

    public void setKeepAliveSeconds(int keepAliveSeconds) {
        synchronized (this.poolSizeMonitor) {
            this.keepAliveSeconds = keepAliveSeconds;
            if (this.threadPoolExecutor != null) {
                this.threadPoolExecutor.setKeepAliveTime(keepAliveSeconds, TimeUnit.SECONDS);
            }
        }
    }

    public int getKeepAliveSeconds() {
        synchronized (this.poolSizeMonitor) {
            return this.keepAliveSeconds;
        }
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public void setAllowCoreThreadTimeOut(boolean allowCoreThreadTimeOut) {
        this.allowCoreThreadTimeOut = allowCoreThreadTimeOut;
    }

    public void setTaskDecorator(TaskDecorator taskDecorator) {
        this.taskDecorator = taskDecorator;
    }

    @Override
    protected ExecutorService initializeExecutor(ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {
        BlockingQueue<Runnable> queue = createQueue(this.queueCapacity);
        ThreadPoolExecutor executor;
        if (this.taskDecorator != null) {
            executor = new ThreadPoolExecutor(this.corePoolSize, this.maxPoolSize, this.keepAliveSeconds, TimeUnit.SECONDS, queue, threadFactory, rejectedExecutionHandler) {
                @Override
                public void execute(Runnable command) {
                    Runnable decorated = taskDecorator.decorate(command);
                    if (decorated != command) {
                        decoratedTaskMap.put(decorated, command);
                    }
                    super.execute(decorated);
                }
            };
        } else {
            executor = new ThreadPoolExecutor(this.corePoolSize, this.maxPoolSize, this.keepAliveSeconds, TimeUnit.SECONDS, queue, threadFactory, rejectedExecutionHandler);

        }
        if (this.allowCoreThreadTimeOut) {
            executor.allowCoreThreadTimeOut(true);
        }
        this.threadPoolExecutor = executor;
        return executor;
    }

    protected BlockingQueue<Runnable> createQueue(int queueCapacity) {
        if (queueCapacity > 0) {
            return new LinkedBlockingQueue<>(queueCapacity);
        } else {
            return new SynchronousQueue<>();
        }
    }

    public ThreadPoolExecutor getThreadPoolExecutor() throws IllegalStateException {
        Assert.state(this.threadPoolExecutor != null, "ThreadPoolTaskExecutor not initialized");
        return this.threadPoolExecutor;
    }

    public int getPoolSize() {
        if (this.threadPoolExecutor == null) {
            // Not initialized yet: assume core pool size.
            return this.corePoolSize;
        }
        return this.threadPoolExecutor.getPoolSize();
    }

    public int getActiveCount() {
        if (this.threadPoolExecutor == null) {
            // Not initialized yet: assume no active threads.
            return 0;
        }
        return this.threadPoolExecutor.getActiveCount();
    }

    @Override
    public void execute(Runnable task) {
        Executor executor = getThreadPoolExecutor();
        try {
            executor.execute(task);
        } catch (RejectedExecutionException ex) {
            throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
        }
    }

    @Override
    public void execute(Runnable task, long startTimeout) {
        execute(task);
    }

    @Override
    public Future<?> submit(Runnable task) {
        ExecutorService executor = getThreadPoolExecutor();
        try {
            return executor.submit(task);
        } catch (RejectedExecutionException ex) {
            throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
        }
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        ExecutorService executor = getThreadPoolExecutor();
        try {
            return executor.submit(task);
        } catch (RejectedExecutionException ex) {
            throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
        }
    }

    @Override
    public ListenableFuture<?> submitListenable(Runnable task) {
        ExecutorService executor = getThreadPoolExecutor();
        try {
            ListenableFutureTask<Object> future = new ListenableFutureTask<>(task, null);
            executor.execute(future);
            return future;
        } catch (RejectedExecutionException ex) {
            throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
        }
    }

    @Override
    public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
        ExecutorService executor = getThreadPoolExecutor();
        try {
            ListenableFutureTask<T> future = new ListenableFutureTask<>(task);
            executor.execute(future);
            return future;
        } catch (RejectedExecutionException ex) {
            throw new TaskRejectedException("Executor [" + executor + "] did not accept task: " + task, ex);
        }
    }

    @Override
    protected void cancelRemainingTask(Runnable task) {
        super.cancelRemainingTask(task);
        // Cancel associated user-level Future handle as well
        Object original = this.decoratedTaskMap.get(task);
        if (original instanceof Future) {
            ((Future<?>) original).cancel(true);
        }
    }

}
