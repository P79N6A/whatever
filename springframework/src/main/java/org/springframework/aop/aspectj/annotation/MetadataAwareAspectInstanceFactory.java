package org.springframework.aop.aspectj.annotation;

import org.springframework.aop.aspectj.AspectInstanceFactory;
import org.springframework.lang.Nullable;

public interface MetadataAwareAspectInstanceFactory extends AspectInstanceFactory {

    AspectMetadata getAspectMetadata();

    @Nullable
    Object getAspectCreationMutex();

}
