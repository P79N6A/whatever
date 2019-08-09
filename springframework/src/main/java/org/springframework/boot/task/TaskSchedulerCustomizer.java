package org.springframework.boot.task;

import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@FunctionalInterface
public interface TaskSchedulerCustomizer {

    void customize(ThreadPoolTaskScheduler taskScheduler);

}
