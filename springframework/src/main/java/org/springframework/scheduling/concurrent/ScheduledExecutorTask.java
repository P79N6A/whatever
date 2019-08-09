package org.springframework.scheduling.concurrent;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.concurrent.TimeUnit;

public class ScheduledExecutorTask {

    @Nullable
    private Runnable runnable;

    private long delay = 0;

    private long period = -1;

    private TimeUnit timeUnit = TimeUnit.MILLISECONDS;

    private boolean fixedRate = false;

    public ScheduledExecutorTask() {
    }

    public ScheduledExecutorTask(Runnable executorTask) {
        this.runnable = executorTask;
    }

    public ScheduledExecutorTask(Runnable executorTask, long delay) {
        this.runnable = executorTask;
        this.delay = delay;
    }

    public ScheduledExecutorTask(Runnable executorTask, long delay, long period, boolean fixedRate) {
        this.runnable = executorTask;
        this.delay = delay;
        this.period = period;
        this.fixedRate = fixedRate;
    }

    public void setRunnable(Runnable executorTask) {
        this.runnable = executorTask;
    }

    public Runnable getRunnable() {
        Assert.state(this.runnable != null, "No Runnable set");
        return this.runnable;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    public long getDelay() {
        return this.delay;
    }

    public void setPeriod(long period) {
        this.period = period;
    }

    public long getPeriod() {
        return this.period;
    }

    public boolean isOneTimeTask() {
        return (this.period <= 0);
    }

    public void setTimeUnit(@Nullable TimeUnit timeUnit) {
        this.timeUnit = (timeUnit != null ? timeUnit : TimeUnit.MILLISECONDS);
    }

    public TimeUnit getTimeUnit() {
        return this.timeUnit;
    }

    public void setFixedRate(boolean fixedRate) {
        this.fixedRate = fixedRate;
    }

    public boolean isFixedRate() {
        return this.fixedRate;
    }

}
