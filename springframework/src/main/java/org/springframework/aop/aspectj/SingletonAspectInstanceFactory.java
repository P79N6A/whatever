package org.springframework.aop.aspectj;

import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.Serializable;

@SuppressWarnings("serial")
public class SingletonAspectInstanceFactory implements AspectInstanceFactory, Serializable {

    private final Object aspectInstance;

    public SingletonAspectInstanceFactory(Object aspectInstance) {
        Assert.notNull(aspectInstance, "Aspect instance must not be null");
        this.aspectInstance = aspectInstance;
    }

    @Override
    public final Object getAspectInstance() {
        return this.aspectInstance;
    }

    @Override
    @Nullable
    public ClassLoader getAspectClassLoader() {
        return this.aspectInstance.getClass().getClassLoader();
    }

    @Override
    public int getOrder() {
        if (this.aspectInstance instanceof Ordered) {
            return ((Ordered) this.aspectInstance).getOrder();
        }
        return getOrderForAspectClass(this.aspectInstance.getClass());
    }

    protected int getOrderForAspectClass(Class<?> aspectClass) {
        return Ordered.LOWEST_PRECEDENCE;
    }

}
