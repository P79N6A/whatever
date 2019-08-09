package org.springframework.aop.aspectj.annotation;

import org.springframework.aop.aspectj.SingletonAspectInstanceFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.OrderUtils;

import java.io.Serializable;

@SuppressWarnings("serial")
public class SingletonMetadataAwareAspectInstanceFactory extends SingletonAspectInstanceFactory implements MetadataAwareAspectInstanceFactory, Serializable {

    private final AspectMetadata metadata;

    public SingletonMetadataAwareAspectInstanceFactory(Object aspectInstance, String aspectName) {
        super(aspectInstance);
        this.metadata = new AspectMetadata(aspectInstance.getClass(), aspectName);
    }

    @Override
    public final AspectMetadata getAspectMetadata() {
        return this.metadata;
    }

    @Override
    public Object getAspectCreationMutex() {
        return this;
    }

    @Override
    protected int getOrderForAspectClass(Class<?> aspectClass) {
        return OrderUtils.getOrder(aspectClass, Ordered.LOWEST_PRECEDENCE);
    }

}
