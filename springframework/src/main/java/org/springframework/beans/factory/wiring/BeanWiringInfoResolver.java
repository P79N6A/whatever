package org.springframework.beans.factory.wiring;

import org.springframework.lang.Nullable;

public interface BeanWiringInfoResolver {

    @Nullable
    BeanWiringInfo resolveWiringInfo(Object beanInstance);

}
