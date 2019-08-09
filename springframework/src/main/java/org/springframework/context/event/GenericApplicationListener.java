package org.springframework.context.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

public interface GenericApplicationListener extends ApplicationListener<ApplicationEvent>, Ordered {

    boolean supportsEventType(ResolvableType eventType);

    default boolean supportsSourceType(@Nullable Class<?> sourceType) {
        return true;
    }

    @Override
    default int getOrder() {
        return LOWEST_PRECEDENCE;
    }

}
