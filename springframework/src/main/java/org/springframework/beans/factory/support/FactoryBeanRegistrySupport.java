package org.springframework.beans.factory.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.FactoryBeanNotInitializedException;
import org.springframework.lang.Nullable;

import java.security.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class FactoryBeanRegistrySupport extends DefaultSingletonBeanRegistry {

    private final Map<String, Object> factoryBeanObjectCache = new ConcurrentHashMap<>(16);

    @Nullable
    protected Class<?> getTypeForFactoryBean(final FactoryBean<?> factoryBean) {
        try {
            if (System.getSecurityManager() != null) {
                return AccessController.doPrivileged((PrivilegedAction<Class<?>>) factoryBean::getObjectType, getAccessControlContext());
            } else {
                return factoryBean.getObjectType();
            }
        } catch (Throwable ex) {
            // Thrown from the FactoryBean's getObjectType implementation.
            logger.info("FactoryBean threw exception from getObjectType, despite the contract saying " + "that it should return null if the type of its object cannot be determined yet", ex);
            return null;
        }
    }

    @Nullable
    protected Object getCachedObjectForFactoryBean(String beanName) {
        return this.factoryBeanObjectCache.get(beanName);
    }

    /**
     *
     */
    protected Object getObjectFromFactoryBean(FactoryBean<?> factory, String beanName, boolean shouldPostProcess) {

        // 单例 && singletonObjects包含该Bean
        if (factory.isSingleton() && containsSingleton(beanName)) {
            synchronized (getSingletonMutex()) {
                // 先从factoryBeanObjectCache获取
                Object object = this.factoryBeanObjectCache.get(beanName);
                if (object == null) {
                    // 缓存没有，FactoryBean#getObject创建
                    object = doGetObjectFromFactoryBean(factory, beanName);
                    // Only post-process and store if not put there already during getObject() call above
                    // (e.g. because of circular reference processing triggered by custom getBean calls)
                    Object alreadyThere = this.factoryBeanObjectCache.get(beanName);
                    if (alreadyThere != null) {
                        object = alreadyThere;
                    } else {
                        if (shouldPostProcess) {
                            // singletonsCurrentlyInCreation
                            if (isSingletonCurrentlyInCreation(beanName)) {
                                // Temporarily return non-post-processed object, not storing it yet..
                                return object;
                            }
                            /*
                             * 创建即将开始
                             * inCreationCheckExclusions不包含beanName && 将beanName添加singletonsCurrentlyInCreation却已存在
                             * 抛出当前Bean正在创建异常
                             */
                            beforeSingletonCreation(beanName);
                            try {
                                // do nothing
                                object = postProcessObjectFromFactoryBean(object, beanName);
                            } catch (Throwable ex) {
                                throw new BeanCreationException(beanName, "Post-processing of FactoryBean's singleton object failed", ex);
                            } finally {
                                /*
                                 * 创建已经完成
                                 * inCreationCheckExclusions不包含beanName && 将beanName从singletonsCurrentlyInCreation移除却不存在
                                 * 抛出当前Bean不在创建异常
                                 */
                                afterSingletonCreation(beanName);
                            }
                        }
                        // singletonObjects包含该Bean
                        if (containsSingleton(beanName)) {
                            // factoryBeanObjectCache
                            this.factoryBeanObjectCache.put(beanName, object);
                        }
                    }
                }
                return object;
            }
        } else {
            Object object = doGetObjectFromFactoryBean(factory, beanName);
            if (shouldPostProcess) {
                try {
                    object = postProcessObjectFromFactoryBean(object, beanName);
                } catch (Throwable ex) {
                    throw new BeanCreationException(beanName, "Post-processing of FactoryBean's object failed", ex);
                }
            }
            return object;
        }
    }

    /**
     * FactoryBean#getObject
     */
    private Object doGetObjectFromFactoryBean(final FactoryBean<?> factory, final String beanName) throws BeanCreationException {
        Object object;
        try {
            if (System.getSecurityManager() != null) {
                AccessControlContext acc = getAccessControlContext();
                try {
                    object = AccessController.doPrivileged((PrivilegedExceptionAction<Object>) factory::getObject, acc);
                } catch (PrivilegedActionException pae) {
                    throw pae.getException();
                }
            } else {
                object = factory.getObject();
            }
        } catch (FactoryBeanNotInitializedException ex) {
            throw new BeanCurrentlyInCreationException(beanName, ex.toString());
        } catch (Throwable ex) {
            throw new BeanCreationException(beanName, "FactoryBean threw exception on object creation", ex);
        }
        // 对于没完全初始化的FactoryBean，不接受null
        if (object == null) {
            //
            if (isSingletonCurrentlyInCreation(beanName)) {
                throw new BeanCurrentlyInCreationException(beanName, "FactoryBean which is currently in creation returned null from getObject");
            }
            object = new NullBean();
        }
        return object;
    }

    protected Object postProcessObjectFromFactoryBean(Object object, String beanName) throws BeansException {
        return object;
    }

    protected FactoryBean<?> getFactoryBean(String beanName, Object beanInstance) throws BeansException {
        if (!(beanInstance instanceof FactoryBean)) {
            throw new BeanCreationException(beanName, "Bean instance of type [" + beanInstance.getClass() + "] is not a FactoryBean");
        }
        return (FactoryBean<?>) beanInstance;
    }

    @Override
    protected void removeSingleton(String beanName) {
        synchronized (getSingletonMutex()) {
            super.removeSingleton(beanName);
            this.factoryBeanObjectCache.remove(beanName);
        }
    }

    @Override
    protected void clearSingletonCache() {
        synchronized (getSingletonMutex()) {
            super.clearSingletonCache();
            this.factoryBeanObjectCache.clear();
        }
    }

    protected AccessControlContext getAccessControlContext() {
        return AccessController.getContext();
    }

}
