package org.springframework.jndi;

import org.springframework.aop.TargetSource;
import org.springframework.lang.Nullable;

import javax.naming.NamingException;

public class JndiObjectTargetSource extends JndiObjectLocator implements TargetSource {

    private boolean lookupOnStartup = true;

    private boolean cache = true;

    @Nullable
    private Object cachedObject;

    @Nullable
    private Class<?> targetClass;

    public void setLookupOnStartup(boolean lookupOnStartup) {
        this.lookupOnStartup = lookupOnStartup;
    }

    public void setCache(boolean cache) {
        this.cache = cache;
    }

    @Override
    public void afterPropertiesSet() throws NamingException {
        super.afterPropertiesSet();
        if (this.lookupOnStartup) {
            Object object = lookup();
            if (this.cache) {
                this.cachedObject = object;
            } else {
                this.targetClass = object.getClass();
            }
        }
    }

    @Override
    @Nullable
    public Class<?> getTargetClass() {
        if (this.cachedObject != null) {
            return this.cachedObject.getClass();
        } else if (this.targetClass != null) {
            return this.targetClass;
        } else {
            return getExpectedType();
        }
    }

    @Override
    public boolean isStatic() {
        return (this.cachedObject != null);
    }

    @Override
    @Nullable
    public Object getTarget() {
        try {
            if (this.lookupOnStartup || !this.cache) {
                return (this.cachedObject != null ? this.cachedObject : lookup());
            } else {
                synchronized (this) {
                    if (this.cachedObject == null) {
                        this.cachedObject = lookup();
                    }
                    return this.cachedObject;
                }
            }
        } catch (NamingException ex) {
            throw new JndiLookupFailureException("JndiObjectTargetSource failed to obtain new target object", ex);
        }
    }

    @Override
    public void releaseTarget(Object target) {
    }

}
