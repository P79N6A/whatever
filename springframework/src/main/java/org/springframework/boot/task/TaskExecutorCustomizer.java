package org.springframework.boot.task;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@FunctionalInterface
public interface TaskExecutorCustomizer {

    void customize(ThreadPoolTaskExecutor taskExecutor);

}
