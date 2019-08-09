package org.springframework.scheduling.concurrent;

import org.springframework.core.task.TaskRejectedException;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.scheduling.support.TaskUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ErrorHandler;

import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import java.util.Date;
import java.util.concurrent.*;

public class ConcurrentTaskScheduler extends ConcurrentTaskExecutor implements TaskScheduler {

    @Nullable
    private static Class<?> managedScheduledExecutorServiceClass;

    static {
        try {
            managedScheduledExecutorServiceClass = ClassUtils.forName("javax.enterprise.concurrent.ManagedScheduledExecutorService", ConcurrentTaskScheduler.class.getClassLoader());
        } catch (ClassNotFoundException ex) {
            // JSR-236 API not available...
            managedScheduledExecutorServiceClass = null;
        }
    }

    private ScheduledExecutorService scheduledExecutor;

    private boolean enterpriseConcurrentScheduler = false;

    @Nullable
    private ErrorHandler errorHandler;

    public ConcurrentTaskScheduler() {
        super();
        this.scheduledExecutor = initScheduledExecutor(null);
    }

    public ConcurrentTaskScheduler(ScheduledExecutorService scheduledExecutor) {
        super(scheduledExecutor);
        this.scheduledExecutor = initScheduledExecutor(scheduledExecutor);
    }

    public ConcurrentTaskScheduler(Executor concurrentExecutor, ScheduledExecutorService scheduledExecutor) {
        super(concurrentExecutor);
        this.scheduledExecutor = initScheduledExecutor(scheduledExecutor);
    }

    private ScheduledExecutorService initScheduledExecutor(@Nullable ScheduledExecutorService scheduledExecutor) {
        if (scheduledExecutor != null) {
            this.scheduledExecutor = scheduledExecutor;
            this.enterpriseConcurrentScheduler = (managedScheduledExecutorServiceClass != null && managedScheduledExecutorServiceClass.isInstance(scheduledExecutor));
        } else {
            this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
            this.enterpriseConcurrentScheduler = false;
        }
        return this.scheduledExecutor;
    }

    public void setScheduledExecutor(@Nullable ScheduledExecutorService scheduledExecutor) {
        initScheduledExecutor(scheduledExecutor);
    }

    public void setErrorHandler(ErrorHandler errorHandler) {
        Assert.notNull(errorHandler, "ErrorHandler must not be null");
        this.errorHandler = errorHandler;
    }

    @Override
    @Nullable
    public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
        try {
            if (this.enterpriseConcurrentScheduler) {
                return new EnterpriseConcurrentTriggerScheduler().schedule(decorateTask(task, true), trigger);
            } else {
                ErrorHandler errorHandler = (this.errorHandler != null ? this.errorHandler : TaskUtils.getDefaultErrorHandler(true));
                return new ReschedulingRunnable(task, trigger, this.scheduledExecutor, errorHandler).schedule();
            }
        } catch (RejectedExecutionException ex) {
            throw new TaskRejectedException("Executor [" + this.scheduledExecutor + "] did not accept task: " + task, ex);
        }
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable task, Date startTime) {
        long initialDelay = startTime.getTime() - System.currentTimeMillis();
        try {
            return this.scheduledExecutor.schedule(decorateTask(task, false), initialDelay, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException ex) {
            throw new TaskRejectedException("Executor [" + this.scheduledExecutor + "] did not accept task: " + task, ex);
        }
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Date startTime, long period) {
        long initialDelay = startTime.getTime() - System.currentTimeMillis();
        try {
            return this.scheduledExecutor.scheduleAtFixedRate(decorateTask(task, true), initialDelay, period, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException ex) {
            throw new TaskRejectedException("Executor [" + this.scheduledExecutor + "] did not accept task: " + task, ex);
        }
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long period) {
        try {
            return this.scheduledExecutor.scheduleAtFixedRate(decorateTask(task, true), 0, period, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException ex) {
            throw new TaskRejectedException("Executor [" + this.scheduledExecutor + "] did not accept task: " + task, ex);
        }
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Date startTime, long delay) {
        long initialDelay = startTime.getTime() - System.currentTimeMillis();
        try {
            return this.scheduledExecutor.scheduleWithFixedDelay(decorateTask(task, true), initialDelay, delay, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException ex) {
            throw new TaskRejectedException("Executor [" + this.scheduledExecutor + "] did not accept task: " + task, ex);
        }
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long delay) {
        try {
            return this.scheduledExecutor.scheduleWithFixedDelay(decorateTask(task, true), 0, delay, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException ex) {
            throw new TaskRejectedException("Executor [" + this.scheduledExecutor + "] did not accept task: " + task, ex);
        }
    }

    private Runnable decorateTask(Runnable task, boolean isRepeatingTask) {
        Runnable result = TaskUtils.decorateTaskWithErrorHandler(task, this.errorHandler, isRepeatingTask);
        if (this.enterpriseConcurrentScheduler) {
            result = ManagedTaskBuilder.buildManagedTask(result, task.toString());
        }
        return result;
    }

    private class EnterpriseConcurrentTriggerScheduler {

        public ScheduledFuture<?> schedule(Runnable task, final Trigger trigger) {
            ManagedScheduledExecutorService executor = (ManagedScheduledExecutorService) scheduledExecutor;
            return executor.schedule(task, new javax.enterprise.concurrent.Trigger() {
                @Override
                @Nullable
                public Date getNextRunTime(@Nullable LastExecution le, Date taskScheduledTime) {
                    return (trigger.nextExecutionTime(le != null ? new SimpleTriggerContext(le.getScheduledStart(), le.getRunStart(), le.getRunEnd()) : new SimpleTriggerContext()));
                }

                @Override
                public boolean skipRun(LastExecution lastExecution, Date scheduledRunTime) {
                    return false;
                }
            });
        }

    }

}
