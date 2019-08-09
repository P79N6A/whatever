package org.springframework.beans.factory.annotation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("serial")
public class InitDestroyAnnotationBeanPostProcessor implements DestructionAwareBeanPostProcessor, MergedBeanDefinitionPostProcessor, PriorityOrdered, Serializable {

    private final transient LifecycleMetadata emptyLifecycleMetadata = new LifecycleMetadata(Object.class, Collections.emptyList(), Collections.emptyList()) {
        @Override
        public void checkConfigMembers(RootBeanDefinition beanDefinition) {
        }

        @Override
        public void invokeInitMethods(Object target, String beanName) {
        }

        @Override
        public void invokeDestroyMethods(Object target, String beanName) {
        }

        @Override
        public boolean hasDestroyMethods() {
            return false;
        }
    };

    protected transient Log logger = LogFactory.getLog(getClass());

    @Nullable
    private Class<? extends Annotation> initAnnotationType;

    @Nullable
    private Class<? extends Annotation> destroyAnnotationType;

    private int order = Ordered.LOWEST_PRECEDENCE;

    @Nullable
    private final transient Map<Class<?>, LifecycleMetadata> lifecycleMetadataCache = new ConcurrentHashMap<>(256);

    public void setInitAnnotationType(Class<? extends Annotation> initAnnotationType) {
        this.initAnnotationType = initAnnotationType;
    }

    public void setDestroyAnnotationType(Class<? extends Annotation> destroyAnnotationType) {
        this.destroyAnnotationType = destroyAnnotationType;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    @Override
    public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
        LifecycleMetadata metadata = findLifecycleMetadata(beanType);
        metadata.checkConfigMembers(beanDefinition);
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        LifecycleMetadata metadata = findLifecycleMetadata(bean.getClass());
        try {
            metadata.invokeInitMethods(bean, beanName);
        } catch (InvocationTargetException ex) {
            throw new BeanCreationException(beanName, "Invocation of init method failed", ex.getTargetException());
        } catch (Throwable ex) {
            throw new BeanCreationException(beanName, "Failed to invoke init method", ex);
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
        LifecycleMetadata metadata = findLifecycleMetadata(bean.getClass());
        try {
            metadata.invokeDestroyMethods(bean, beanName);
        } catch (InvocationTargetException ex) {
            String msg = "Destroy method on bean with name '" + beanName + "' threw an exception";
            if (logger.isDebugEnabled()) {
                logger.warn(msg, ex.getTargetException());
            } else {
                logger.warn(msg + ": " + ex.getTargetException());
            }
        } catch (Throwable ex) {
            logger.warn("Failed to invoke destroy method on bean with name '" + beanName + "'", ex);
        }
    }

    @Override
    public boolean requiresDestruction(Object bean) {
        return findLifecycleMetadata(bean.getClass()).hasDestroyMethods();
    }

    private LifecycleMetadata findLifecycleMetadata(Class<?> clazz) {
        if (this.lifecycleMetadataCache == null) {
            // Happens after deserialization, during destruction...
            return buildLifecycleMetadata(clazz);
        }
        // Quick check on the concurrent map first, with minimal locking.
        LifecycleMetadata metadata = this.lifecycleMetadataCache.get(clazz);
        if (metadata == null) {
            synchronized (this.lifecycleMetadataCache) {
                metadata = this.lifecycleMetadataCache.get(clazz);
                if (metadata == null) {
                    metadata = buildLifecycleMetadata(clazz);
                    this.lifecycleMetadataCache.put(clazz, metadata);
                }
                return metadata;
            }
        }
        return metadata;
    }

    private LifecycleMetadata buildLifecycleMetadata(final Class<?> clazz) {
        if (!AnnotationUtils.isCandidateClass(clazz, Arrays.asList(this.initAnnotationType, this.destroyAnnotationType))) {
            return this.emptyLifecycleMetadata;
        }
        List<LifecycleElement> initMethods = new ArrayList<>();
        List<LifecycleElement> destroyMethods = new ArrayList<>();
        Class<?> targetClass = clazz;
        do {
            final List<LifecycleElement> currInitMethods = new ArrayList<>();
            final List<LifecycleElement> currDestroyMethods = new ArrayList<>();
            ReflectionUtils.doWithLocalMethods(targetClass, method -> {
                if (this.initAnnotationType != null && method.isAnnotationPresent(this.initAnnotationType)) {
                    LifecycleElement element = new LifecycleElement(method);
                    currInitMethods.add(element);
                    if (logger.isTraceEnabled()) {
                        logger.trace("Found init method on class [" + clazz.getName() + "]: " + method);
                    }
                }
                if (this.destroyAnnotationType != null && method.isAnnotationPresent(this.destroyAnnotationType)) {
                    currDestroyMethods.add(new LifecycleElement(method));
                    if (logger.isTraceEnabled()) {
                        logger.trace("Found destroy method on class [" + clazz.getName() + "]: " + method);
                    }
                }
            });
            initMethods.addAll(0, currInitMethods);
            destroyMethods.addAll(currDestroyMethods);
            targetClass = targetClass.getSuperclass();
        } while (targetClass != null && targetClass != Object.class);
        return (initMethods.isEmpty() && destroyMethods.isEmpty() ? this.emptyLifecycleMetadata : new LifecycleMetadata(clazz, initMethods, destroyMethods));
    }
    //---------------------------------------------------------------------
    // Serialization support
    //---------------------------------------------------------------------

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        // Rely on default serialization; just initialize state after deserialization.
        ois.defaultReadObject();
        // Initialize transient fields.
        this.logger = LogFactory.getLog(getClass());
    }

    private class LifecycleMetadata {

        private final Class<?> targetClass;

        private final Collection<LifecycleElement> initMethods;

        private final Collection<LifecycleElement> destroyMethods;

        @Nullable
        private volatile Set<LifecycleElement> checkedInitMethods;

        @Nullable
        private volatile Set<LifecycleElement> checkedDestroyMethods;

        public LifecycleMetadata(Class<?> targetClass, Collection<LifecycleElement> initMethods, Collection<LifecycleElement> destroyMethods) {
            this.targetClass = targetClass;
            this.initMethods = initMethods;
            this.destroyMethods = destroyMethods;
        }

        public void checkConfigMembers(RootBeanDefinition beanDefinition) {
            Set<LifecycleElement> checkedInitMethods = new LinkedHashSet<>(this.initMethods.size());
            for (LifecycleElement element : this.initMethods) {
                String methodIdentifier = element.getIdentifier();
                if (!beanDefinition.isExternallyManagedInitMethod(methodIdentifier)) {
                    beanDefinition.registerExternallyManagedInitMethod(methodIdentifier);
                    checkedInitMethods.add(element);
                    if (logger.isTraceEnabled()) {
                        logger.trace("Registered init method on class [" + this.targetClass.getName() + "]: " + element);
                    }
                }
            }
            Set<LifecycleElement> checkedDestroyMethods = new LinkedHashSet<>(this.destroyMethods.size());
            for (LifecycleElement element : this.destroyMethods) {
                String methodIdentifier = element.getIdentifier();
                if (!beanDefinition.isExternallyManagedDestroyMethod(methodIdentifier)) {
                    beanDefinition.registerExternallyManagedDestroyMethod(methodIdentifier);
                    checkedDestroyMethods.add(element);
                    if (logger.isTraceEnabled()) {
                        logger.trace("Registered destroy method on class [" + this.targetClass.getName() + "]: " + element);
                    }
                }
            }
            this.checkedInitMethods = checkedInitMethods;
            this.checkedDestroyMethods = checkedDestroyMethods;
        }

        public void invokeInitMethods(Object target, String beanName) throws Throwable {
            Collection<LifecycleElement> checkedInitMethods = this.checkedInitMethods;
            Collection<LifecycleElement> initMethodsToIterate = (checkedInitMethods != null ? checkedInitMethods : this.initMethods);
            if (!initMethodsToIterate.isEmpty()) {
                for (LifecycleElement element : initMethodsToIterate) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Invoking init method on bean '" + beanName + "': " + element.getMethod());
                    }
                    element.invoke(target);
                }
            }
        }

        public void invokeDestroyMethods(Object target, String beanName) throws Throwable {
            Collection<LifecycleElement> checkedDestroyMethods = this.checkedDestroyMethods;
            Collection<LifecycleElement> destroyMethodsToUse = (checkedDestroyMethods != null ? checkedDestroyMethods : this.destroyMethods);
            if (!destroyMethodsToUse.isEmpty()) {
                for (LifecycleElement element : destroyMethodsToUse) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Invoking destroy method on bean '" + beanName + "': " + element.getMethod());
                    }
                    element.invoke(target);
                }
            }
        }

        public boolean hasDestroyMethods() {
            Collection<LifecycleElement> checkedDestroyMethods = this.checkedDestroyMethods;
            Collection<LifecycleElement> destroyMethodsToUse = (checkedDestroyMethods != null ? checkedDestroyMethods : this.destroyMethods);
            return !destroyMethodsToUse.isEmpty();
        }

    }

    private static class LifecycleElement {

        private final Method method;

        private final String identifier;

        public LifecycleElement(Method method) {
            if (method.getParameterCount() != 0) {
                throw new IllegalStateException("Lifecycle method annotation requires a no-arg method: " + method);
            }
            this.method = method;
            this.identifier = (Modifier.isPrivate(method.getModifiers()) ? ClassUtils.getQualifiedMethodName(method) : method.getName());
        }

        public Method getMethod() {
            return this.method;
        }

        public String getIdentifier() {
            return this.identifier;
        }

        public void invoke(Object target) throws Throwable {
            ReflectionUtils.makeAccessible(this.method);
            this.method.invoke(target, (Object[]) null);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof LifecycleElement)) {
                return false;
            }
            LifecycleElement otherElement = (LifecycleElement) other;
            return (this.identifier.equals(otherElement.identifier));
        }

        @Override
        public int hashCode() {
            return this.identifier.hashCode();
        }

    }

}
