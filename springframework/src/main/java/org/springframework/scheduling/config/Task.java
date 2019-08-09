package org.springframework.scheduling.config;

import org.springframework.util.Assert;

public class Task {

    private final Runnable runnable;

    public Task(Runnable runnable) {
        Assert.notNull(runnable, "Runnable must not be null");
        this.runnable = runnable;
    }

    public Runnable getRunnable() {
        return this.runnable;
    }

    @Override
    public String toString() {
        return this.runnable.toString();
    }

}
