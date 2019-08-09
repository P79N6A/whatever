package org.springframework.context.event;

import org.springframework.context.ApplicationContext;

@SuppressWarnings("serial")
public class ContextClosedEvent extends ApplicationContextEvent {

    public ContextClosedEvent(ApplicationContext source) {
        super(source);
    }

}
