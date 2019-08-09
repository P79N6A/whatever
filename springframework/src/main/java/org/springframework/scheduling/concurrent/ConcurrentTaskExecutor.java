package org.springframework.scheduling.concurrent;

import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.SchedulingAwareRunnable;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.util.ClassUtils;
import org.springframework.util.concurrent.ListenableFuture;

import javax.enterprise.concurrent.ManagedExecutors;
import javax.enterprise.concurrent.ManagedTask;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ConcurrentTaskExecutor implements AsyncListenableTaskExecutor, SchedulingTaskExecutor {

    @Nullable
    private static Class<?> managedExecutorServiceClass;

    static {
        try {
            managedExecutorServiceClass = ClassUtils.forName("javax.enterprise.concurrent.ManagedExecutorService", ConcurrentTaskScheduler.class.getClassLoader());
        } catch (ClassNotFoundException ex) {
            // JSR-236 API not available...
            managedExecutorServiceClass = null;
        }
    }

    private Executor concurrentExecutor;

    private TaskExecutorAdapter adaptedExecutor;

    public ConcurrentTaskExecutor() {
        this.concurrentExecutor = Executors.newSingleThreadExecutor();
        this.adaptedExecutor = new TaskExecutorAdapter(this.concurrentExecutor);
    }

    public ConcurrentTaskExecutor(@Nullable Executor executor) {
        this.concurrentExecutor = (executor != null ? executor : Executors.newSingleThreadExecutor());
        this.adaptedExecutor = getAdaptedExecutor(this.concurrentExecutor);
    }

    public final void setConcurrentExecutor(@Nullable Executor executor) {
        this.concurrentExecutor = (executor != null ? executor : Executors.newSingleThreadExecutor());
        this.adaptedExecutor = getAdaptedExecutor(this.concurrentExecutor);
    }

    public final Executor getConcurrentExecutor() {
        return this.concurrentExecutor;
    }

    public final void setTaskDecorator(TaskDecorator taskDecorator) {
        this.adaptedExecutor.setTaskDecorator(taskDecorator);
    }

    @Override
    public void execute(Runnable task) {
        this.adaptedExecutor.execute(task);
    }

    @Override
    public void execute(Runnable task, long startTimeout) {
        this.adaptedExecutor.execute(task, startTimeout);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return this.adaptedExecutor.submit(task);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return this.adaptedExecutor.submit(task);
    }

    @Override
    public ListenableFuture<?> submitListenable(Runnable task) {
        return this.adaptedExecutor.submitListenable(task);
    }

    @Override
    public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
        return this.adaptedExecutor.submitListenable(task);
    }

    private static TaskExecutorAdapter getAdaptedExecutor(Executor concurrentExecutor) {
        if (managedExecutorServiceClass != null && managedExecutorServiceClass.isInstance(concurrentExecutor)) {
            return new ManagedTaskExecutorAdapter(concurrentExecutor);
        }
        return new TaskExecutorAdapter(concurrentExecutor);
    }

    private static class ManagedTaskExecutorAdapter extends TaskExecutorAdapter {

        public ManagedTaskExecutorAdapter(Executor concurrentExecutor) {
            super(concurrentExecutor);
        }

        @Override
        public void execute(Runnable task) {
            super.execute(ManagedTaskBuilder.buildManagedTask(task, task.toString()));
        }

        @Override
        public Future<?> submit(Runnable task) {
            return super.submit(ManagedTaskBuilder.buildManagedTask(task, task.toString()));
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            return super.submit(ManagedTaskBuilder.buildManagedTask(task, task.toString()));
        }

        @Override
        public ListenableFuture<?> submitListenable(Runnable task) {
            return super.submitListenable(ManagedTaskBuilder.buildManagedTask(task, task.toString()));
        }

        @Override
        public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
            return super.submitListenable(ManagedTaskBuilder.buildManagedTask(task, task.toString()));
        }

    }

    protected static class ManagedTaskBuilder {

        public static Runnable buildManagedTask(Runnable task, String identityName) {
            Map<String, String> properties;
            if (task instanceof SchedulingAwareRunnable) {
                properties = new HashMap<>(4);
                properties.put(ManagedTask.LONGRUNNING_HINT, Boolean.toString(((SchedulingAwareRunnable) task).isLongLived()));
            } else {
                properties = new HashMap<>(2);
            }
            properties.put(ManagedTask.IDENTITY_NAME, identityName);
            return ManagedExecutors.managedTask(task, properties, null);
        }

        public static <T> Callable<T> buildManagedTask(Callable<T> task, String identityName) {
            Map<String, String> properties = new HashMap<>(2);
            properties.put(ManagedTask.IDENTITY_NAME, identityName);
            return ManagedExecutors.managedTask(task, properties, null);
        }

    }

}
