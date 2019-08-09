package org.springframework.scheduling.config;

import org.springframework.beans.factory.SmartInitializingSingleton;

public class ContextLifecycleScheduledTaskRegistrar extends ScheduledTaskRegistrar implements SmartInitializingSingleton {

    @Override
    public void afterPropertiesSet() {
        // no-op
    }

    @Override
    public void afterSingletonsInstantiated() {
        scheduleTasks();
    }

}
