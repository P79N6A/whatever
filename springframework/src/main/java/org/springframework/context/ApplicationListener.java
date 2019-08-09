package org.springframework.context;

import java.util.EventListener;

@FunctionalInterface
public interface ApplicationListener<E extends ApplicationEvent> extends EventListener {

    void onApplicationEvent(E event);

}
