package org.springframework.objenesis;

import org.springframework.core.SpringProperties;
import org.springframework.objenesis.instantiator.ObjectInstantiator;
import org.springframework.objenesis.strategy.InstantiatorStrategy;
import org.springframework.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.util.ConcurrentReferenceHashMap;

public class SpringObjenesis implements Objenesis {

    public static final String IGNORE_OBJENESIS_PROPERTY_NAME = "spring.objenesis.ignore";

    private final InstantiatorStrategy strategy;

    private final ConcurrentReferenceHashMap<Class<?>, ObjectInstantiator<?>> cache = new ConcurrentReferenceHashMap<>();

    private volatile Boolean worthTrying;

    public SpringObjenesis() {
        this(null);
    }

    public SpringObjenesis(InstantiatorStrategy strategy) {
        this.strategy = (strategy != null ? strategy : new StdInstantiatorStrategy());
        // Evaluate the "spring.objenesis.ignore" property upfront...
        if (SpringProperties.getFlag(SpringObjenesis.IGNORE_OBJENESIS_PROPERTY_NAME)) {
            this.worthTrying = Boolean.FALSE;
        }
    }

    public boolean isWorthTrying() {
        return (this.worthTrying != Boolean.FALSE);
    }

    public <T> T newInstance(Class<T> clazz, boolean useCache) {
        if (!useCache) {
            return newInstantiatorOf(clazz).newInstance();
        }
        return getInstantiatorOf(clazz).newInstance();
    }

    public <T> T newInstance(Class<T> clazz) {
        return getInstantiatorOf(clazz).newInstance();
    }

    @SuppressWarnings("unchecked")
    public <T> ObjectInstantiator<T> getInstantiatorOf(Class<T> clazz) {
        ObjectInstantiator<?> instantiator = this.cache.get(clazz);
        if (instantiator == null) {
            ObjectInstantiator<T> newInstantiator = newInstantiatorOf(clazz);
            instantiator = this.cache.putIfAbsent(clazz, newInstantiator);
            if (instantiator == null) {
                instantiator = newInstantiator;
            }
        }
        return (ObjectInstantiator<T>) instantiator;
    }

    protected <T> ObjectInstantiator<T> newInstantiatorOf(Class<T> clazz) {
        Boolean currentWorthTrying = this.worthTrying;
        try {
            ObjectInstantiator<T> instantiator = this.strategy.newInstantiatorOf(clazz);
            if (currentWorthTrying == null) {
                this.worthTrying = Boolean.TRUE;
            }
            return instantiator;
        } catch (ObjenesisException ex) {
            if (currentWorthTrying == null) {
                Throwable cause = ex.getCause();
                if (cause instanceof ClassNotFoundException || cause instanceof IllegalAccessException) {
                    // Indicates that the chosen instantiation strategy does not work on the given JVM.
                    // Typically a failure to initialize the default SunReflectionFactoryInstantiator.
                    // Let's assume that any subsequent attempts to use Objenesis will fail as well...
                    this.worthTrying = Boolean.FALSE;
                }
            }
            throw ex;
        } catch (NoClassDefFoundError err) {
            // Happening on the production version of Google App Engine, coming out of the
            // restricted "sun.reflect.ReflectionFactory" class...
            if (currentWorthTrying == null) {
                this.worthTrying = Boolean.FALSE;
            }
            throw new ObjenesisException(err);
        }
    }

}
