package org.springframework.beans.factory.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.core.OrderComparator;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Stream;

public class StaticListableBeanFactory implements ListableBeanFactory {

    private final Map<String, Object> beans;

    public StaticListableBeanFactory() {
        this.beans = new LinkedHashMap<>();
    }

    public StaticListableBeanFactory(Map<String, Object> beans) {
        Assert.notNull(beans, "Beans Map must not be null");
        this.beans = beans;
    }

    public void addBean(String name, Object bean) {
        this.beans.put(name, bean);
    }
    //---------------------------------------------------------------------
    // Implementation of BeanFactory interface
    //---------------------------------------------------------------------

    @Override
    public Object getBean(String name) throws BeansException {
        String beanName = BeanFactoryUtils.transformedBeanName(name);
        Object bean = this.beans.get(beanName);
        if (bean == null) {
            throw new NoSuchBeanDefinitionException(beanName, "Defined beans are [" + StringUtils.collectionToCommaDelimitedString(this.beans.keySet()) + "]");
        }
        // Don't let calling code try to dereference the
        // bean factory if the bean isn't a factory
        if (BeanFactoryUtils.isFactoryDereference(name) && !(bean instanceof FactoryBean)) {
            throw new BeanIsNotAFactoryException(beanName, bean.getClass());
        }
        if (bean instanceof FactoryBean && !BeanFactoryUtils.isFactoryDereference(name)) {
            try {
                Object exposedObject = ((FactoryBean<?>) bean).getObject();
                if (exposedObject == null) {
                    throw new BeanCreationException(beanName, "FactoryBean exposed null object");
                }
                return exposedObject;
            } catch (Exception ex) {
                throw new BeanCreationException(beanName, "FactoryBean threw exception on object creation", ex);
            }
        } else {
            return bean;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getBean(String name, @Nullable Class<T> requiredType) throws BeansException {
        Object bean = getBean(name);
        if (requiredType != null && !requiredType.isInstance(bean)) {
            throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
        }
        return (T) bean;
    }

    @Override
    public Object getBean(String name, Object... args) throws BeansException {
        if (!ObjectUtils.isEmpty(args)) {
            throw new UnsupportedOperationException("StaticListableBeanFactory does not support explicit bean creation arguments");
        }
        return getBean(name);
    }

    @Override
    public <T> T getBean(Class<T> requiredType) throws BeansException {
        String[] beanNames = getBeanNamesForType(requiredType);
        if (beanNames.length == 1) {
            return getBean(beanNames[0], requiredType);
        } else if (beanNames.length > 1) {
            throw new NoUniqueBeanDefinitionException(requiredType, beanNames);
        } else {
            throw new NoSuchBeanDefinitionException(requiredType);
        }
    }

    @Override
    public <T> T getBean(Class<T> requiredType, Object... args) throws BeansException {
        if (!ObjectUtils.isEmpty(args)) {
            throw new UnsupportedOperationException("StaticListableBeanFactory does not support explicit bean creation arguments");
        }
        return getBean(requiredType);
    }

    @Override
    public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType) throws BeansException {
        return getBeanProvider(ResolvableType.forRawClass(requiredType));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType) {
        return new ObjectProvider<T>() {
            @Override
            public T getObject() throws BeansException {
                String[] beanNames = getBeanNamesForType(requiredType);
                if (beanNames.length == 1) {
                    return (T) getBean(beanNames[0], requiredType);
                } else if (beanNames.length > 1) {
                    throw new NoUniqueBeanDefinitionException(requiredType, beanNames);
                } else {
                    throw new NoSuchBeanDefinitionException(requiredType);
                }
            }

            @Override
            public T getObject(Object... args) throws BeansException {
                String[] beanNames = getBeanNamesForType(requiredType);
                if (beanNames.length == 1) {
                    return (T) getBean(beanNames[0], args);
                } else if (beanNames.length > 1) {
                    throw new NoUniqueBeanDefinitionException(requiredType, beanNames);
                } else {
                    throw new NoSuchBeanDefinitionException(requiredType);
                }
            }

            @Override
            @Nullable
            public T getIfAvailable() throws BeansException {
                String[] beanNames = getBeanNamesForType(requiredType);
                if (beanNames.length == 1) {
                    return (T) getBean(beanNames[0]);
                } else if (beanNames.length > 1) {
                    throw new NoUniqueBeanDefinitionException(requiredType, beanNames);
                } else {
                    return null;
                }
            }

            @Override
            @Nullable
            public T getIfUnique() throws BeansException {
                String[] beanNames = getBeanNamesForType(requiredType);
                if (beanNames.length == 1) {
                    return (T) getBean(beanNames[0]);
                } else {
                    return null;
                }
            }

            @Override
            public Stream<T> stream() {
                return Arrays.stream(getBeanNamesForType(requiredType)).map(name -> (T) getBean(name));
            }

            @Override
            public Stream<T> orderedStream() {
                return stream().sorted(OrderComparator.INSTANCE);
            }
        };
    }

    @Override
    public boolean containsBean(String name) {
        return this.beans.containsKey(name);
    }

    @Override
    public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
        Object bean = getBean(name);
        // In case of FactoryBean, return singleton status of created object.
        return (bean instanceof FactoryBean && ((FactoryBean<?>) bean).isSingleton());
    }

    @Override
    public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
        Object bean = getBean(name);
        // In case of FactoryBean, return prototype status of created object.
        return ((bean instanceof SmartFactoryBean && ((SmartFactoryBean<?>) bean).isPrototype()) || (bean instanceof FactoryBean && !((FactoryBean<?>) bean).isSingleton()));
    }

    @Override
    public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
        Class<?> type = getType(name);
        return (type != null && typeToMatch.isAssignableFrom(type));
    }

    @Override
    public boolean isTypeMatch(String name, @Nullable Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
        Class<?> type = getType(name);
        return (typeToMatch == null || (type != null && typeToMatch.isAssignableFrom(type)));
    }

    @Override
    public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
        String beanName = BeanFactoryUtils.transformedBeanName(name);
        Object bean = this.beans.get(beanName);
        if (bean == null) {
            throw new NoSuchBeanDefinitionException(beanName, "Defined beans are [" + StringUtils.collectionToCommaDelimitedString(this.beans.keySet()) + "]");
        }
        if (bean instanceof FactoryBean && !BeanFactoryUtils.isFactoryDereference(name)) {
            // If it's a FactoryBean, we want to look at what it creates, not the factory class.
            return ((FactoryBean<?>) bean).getObjectType();
        }
        return bean.getClass();
    }

    @Override
    public String[] getAliases(String name) {
        return new String[0];
    }
    //---------------------------------------------------------------------
    // Implementation of ListableBeanFactory interface
    //---------------------------------------------------------------------

    @Override
    public boolean containsBeanDefinition(String name) {
        return this.beans.containsKey(name);
    }

    @Override
    public int getBeanDefinitionCount() {
        return this.beans.size();
    }

    @Override
    public String[] getBeanDefinitionNames() {
        return StringUtils.toStringArray(this.beans.keySet());
    }

    @Override
    public String[] getBeanNamesForType(@Nullable ResolvableType type) {
        boolean isFactoryType = false;
        if (type != null) {
            Class<?> resolved = type.resolve();
            if (resolved != null && FactoryBean.class.isAssignableFrom(resolved)) {
                isFactoryType = true;
            }
        }
        List<String> matches = new ArrayList<>();
        for (Map.Entry<String, Object> entry : this.beans.entrySet()) {
            String name = entry.getKey();
            Object beanInstance = entry.getValue();
            if (beanInstance instanceof FactoryBean && !isFactoryType) {
                Class<?> objectType = ((FactoryBean<?>) beanInstance).getObjectType();
                if (objectType != null && (type == null || type.isAssignableFrom(objectType))) {
                    matches.add(name);
                }
            } else {
                if (type == null || type.isInstance(beanInstance)) {
                    matches.add(name);
                }
            }
        }
        return StringUtils.toStringArray(matches);
    }

    @Override
    public String[] getBeanNamesForType(@Nullable Class<?> type) {
        return getBeanNamesForType(ResolvableType.forClass(type));
    }

    @Override
    public String[] getBeanNamesForType(@Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
        return getBeanNamesForType(ResolvableType.forClass(type));
    }

    @Override
    public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type) throws BeansException {
        return getBeansOfType(type, true, true);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type, boolean includeNonSingletons, boolean allowEagerInit) throws BeansException {
        boolean isFactoryType = (type != null && FactoryBean.class.isAssignableFrom(type));
        Map<String, T> matches = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : this.beans.entrySet()) {
            String beanName = entry.getKey();
            Object beanInstance = entry.getValue();
            // Is bean a FactoryBean?
            if (beanInstance instanceof FactoryBean && !isFactoryType) {
                // Match object created by FactoryBean.
                FactoryBean<?> factory = (FactoryBean<?>) beanInstance;
                Class<?> objectType = factory.getObjectType();
                if ((includeNonSingletons || factory.isSingleton()) && objectType != null && (type == null || type.isAssignableFrom(objectType))) {
                    matches.put(beanName, getBean(beanName, type));
                }
            } else {
                if (type == null || type.isInstance(beanInstance)) {
                    // If type to match is FactoryBean, return FactoryBean itself.
                    // Else, return bean instance.
                    if (isFactoryType) {
                        beanName = FACTORY_BEAN_PREFIX + beanName;
                    }
                    matches.put(beanName, (T) beanInstance);
                }
            }
        }
        return matches;
    }

    @Override
    public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
        List<String> results = new ArrayList<>();
        for (String beanName : this.beans.keySet()) {
            if (findAnnotationOnBean(beanName, annotationType) != null) {
                results.add(beanName);
            }
        }
        return StringUtils.toStringArray(results);
    }

    @Override
    public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) throws BeansException {
        Map<String, Object> results = new LinkedHashMap<>();
        for (String beanName : this.beans.keySet()) {
            if (findAnnotationOnBean(beanName, annotationType) != null) {
                results.put(beanName, getBean(beanName));
            }
        }
        return results;
    }

    @Override
    @Nullable
    public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType) throws NoSuchBeanDefinitionException {
        Class<?> beanType = getType(beanName);
        return (beanType != null ? AnnotationUtils.findAnnotation(beanType, annotationType) : null);
    }

}
