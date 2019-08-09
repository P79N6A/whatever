package org.springframework.context.event;

import org.springframework.context.ApplicationContext;

@SuppressWarnings("serial")
public class ContextStoppedEvent extends ApplicationContextEvent {

    public ContextStoppedEvent(ApplicationContext source) {
        super(source);
    }

}
