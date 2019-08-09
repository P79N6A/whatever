package org.springframework.aop.framework.autoproxy;

import org.springframework.aop.TargetSource;
import org.springframework.lang.Nullable;

@FunctionalInterface
public interface TargetSourceCreator {

    @Nullable
    TargetSource getTargetSource(Class<?> beanClass, String beanName);

}
