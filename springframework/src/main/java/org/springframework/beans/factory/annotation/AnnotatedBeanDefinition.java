package org.springframework.beans.factory.annotation;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.lang.Nullable;

public interface AnnotatedBeanDefinition extends BeanDefinition {

    AnnotationMetadata getMetadata();

    @Nullable
    MethodMetadata getFactoryMethodMetadata();

}
