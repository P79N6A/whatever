package org.springframework.boot.logging;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

public class LoggingInitializationContext {

    private final ConfigurableEnvironment environment;

    public LoggingInitializationContext(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    public Environment getEnvironment() {
        return this.environment;
    }

}
