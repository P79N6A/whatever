package org.springframework.boot.context.event;

import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;

@SuppressWarnings("serial")
public class ApplicationEnvironmentPreparedEvent extends SpringApplicationEvent {

    private final ConfigurableEnvironment environment;

    public ApplicationEnvironmentPreparedEvent(SpringApplication application, String[] args, ConfigurableEnvironment environment) {
        super(application, args);
        this.environment = environment;
    }

    public ConfigurableEnvironment getEnvironment() {
        return this.environment;
    }

}
