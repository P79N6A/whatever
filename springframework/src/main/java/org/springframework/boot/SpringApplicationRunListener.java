package org.springframework.boot;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

public interface SpringApplicationRunListener {

    default void starting() {
    }

    default void environmentPrepared(ConfigurableEnvironment environment) {
    }

    default void contextPrepared(ConfigurableApplicationContext context) {
    }

    default void contextLoaded(ConfigurableApplicationContext context) {
    }

    default void started(ConfigurableApplicationContext context) {
    }

    default void running(ConfigurableApplicationContext context) {
    }

    default void failed(ConfigurableApplicationContext context, Throwable exception) {
    }

}
