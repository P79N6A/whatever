package org.springframework.boot.context.event;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationEvent;

@SuppressWarnings("serial")
public abstract class SpringApplicationEvent extends ApplicationEvent {

    private final String[] args;

    public SpringApplicationEvent(SpringApplication application, String[] args) {
        super(application);
        this.args = args;
    }

    public SpringApplication getSpringApplication() {
        return (SpringApplication) getSource();
    }

    public final String[] getArgs() {
        return this.args;
    }

}
