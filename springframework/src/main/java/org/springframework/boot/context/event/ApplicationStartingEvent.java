package org.springframework.boot.context.event;

import org.springframework.boot.SpringApplication;

@SuppressWarnings("serial")
public class ApplicationStartingEvent extends SpringApplicationEvent {

    public ApplicationStartingEvent(SpringApplication application, String[] args) {
        super(application, args);
    }

}
