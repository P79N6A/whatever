package org.springframework.core.task;

import org.springframework.util.Assert;

import java.io.Serializable;

@SuppressWarnings("serial")
public class SyncTaskExecutor implements TaskExecutor, Serializable {

    @Override
    public void execute(Runnable task) {
        Assert.notNull(task, "Runnable must not be null");
        task.run();
    }

}
