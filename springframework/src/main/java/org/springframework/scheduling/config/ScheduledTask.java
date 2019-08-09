package org.springframework.scheduling.config;

import org.springframework.lang.Nullable;

import java.util.concurrent.ScheduledFuture;

public final class ScheduledTask {

    private final Task task;

    @Nullable
    volatile ScheduledFuture<?> future;

    ScheduledTask(Task task) {
        this.task = task;
    }

    public Task getTask() {
        return this.task;
    }

    public void cancel() {
        ScheduledFuture<?> future = this.future;
        if (future != null) {
            future.cancel(true);
        }
    }

    @Override
    public String toString() {
        return this.task.toString();
    }

}
