package org.springframework.context.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;

public interface SmartApplicationListener extends ApplicationListener<ApplicationEvent>, Ordered {

    boolean supportsEventType(Class<? extends ApplicationEvent> eventType);

    default boolean supportsSourceType(@Nullable Class<?> sourceType) {
        return true;
    }

    @Override
    default int getOrder() {
        return LOWEST_PRECEDENCE;
    }

}
