package org.springframework.aop;

import org.springframework.lang.Nullable;

public interface TargetClassAware {

    @Nullable
    Class<?> getTargetClass();

}
