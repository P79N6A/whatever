package org.springframework.context.event;

import org.springframework.context.ApplicationContext;

@SuppressWarnings("serial")
public class ContextRefreshedEvent extends ApplicationContextEvent {

    public ContextRefreshedEvent(ApplicationContext source) {
        super(source);
    }

}
