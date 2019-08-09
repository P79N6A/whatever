package org.springframework.aop.aspectj.annotation;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.Serializable;

@SuppressWarnings("serial")
public class LazySingletonAspectInstanceFactoryDecorator implements MetadataAwareAspectInstanceFactory, Serializable {

    private final MetadataAwareAspectInstanceFactory maaif;

    @Nullable
    private volatile Object materialized;

    public LazySingletonAspectInstanceFactoryDecorator(MetadataAwareAspectInstanceFactory maaif) {
        Assert.notNull(maaif, "AspectInstanceFactory must not be null");
        this.maaif = maaif;
    }

    @Override
    public Object getAspectInstance() {
        Object aspectInstance = this.materialized;
        if (aspectInstance == null) {
            Object mutex = this.maaif.getAspectCreationMutex();
            if (mutex == null) {
                aspectInstance = this.maaif.getAspectInstance();
                this.materialized = aspectInstance;
            } else {
                synchronized (mutex) {
                    aspectInstance = this.materialized;
                    if (aspectInstance == null) {
                        aspectInstance = this.maaif.getAspectInstance();
                        this.materialized = aspectInstance;
                    }
                }
            }
        }
        return aspectInstance;
    }

    public boolean isMaterialized() {
        return (this.materialized != null);
    }

    @Override
    @Nullable
    public ClassLoader getAspectClassLoader() {
        return this.maaif.getAspectClassLoader();
    }

    @Override
    public AspectMetadata getAspectMetadata() {
        return this.maaif.getAspectMetadata();
    }

    @Override
    @Nullable
    public Object getAspectCreationMutex() {
        return this.maaif.getAspectCreationMutex();
    }

    @Override
    public int getOrder() {
        return this.maaif.getOrder();
    }

    @Override
    public String toString() {
        return "LazySingletonAspectInstanceFactoryDecorator: decorating " + this.maaif;
    }

}
