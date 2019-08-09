package org.springframework.boot.context.event;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SuppressWarnings("serial")
public class ApplicationReadyEvent extends SpringApplicationEvent {

    private final ConfigurableApplicationContext context;

    public ApplicationReadyEvent(SpringApplication application, String[] args, ConfigurableApplicationContext context) {
        super(application, args);
        this.context = context;
    }

    public ConfigurableApplicationContext getApplicationContext() {
        return this.context;
    }

}
