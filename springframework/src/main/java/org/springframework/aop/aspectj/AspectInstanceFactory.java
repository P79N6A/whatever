package org.springframework.aop.aspectj;

import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;

public interface AspectInstanceFactory extends Ordered {

    Object getAspectInstance();

    @Nullable
    ClassLoader getAspectClassLoader();

}
