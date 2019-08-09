package org.springframework.boot.autoconfigure.condition;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.StandardMethodMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

final class BeanTypeRegistry implements SmartInitializingSingleton {

    private static final Log logger = LogFactory.getLog(BeanTypeRegistry.class);

    static final String FACTORY_BEAN_OBJECT_TYPE = "factoryBeanObjectType";

    private static final String BEAN_NAME = BeanTypeRegistry.class.getName();

    private final DefaultListableBeanFactory beanFactory;

    private final Map<String, ResolvableType> beanTypes = new HashMap<>();

    private final Map<String, RootBeanDefinition> beanDefinitions = new HashMap<>();

    private BeanTypeRegistry(DefaultListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    public Set<String> getNamesForType(Class<?> type, TypeExtractor typeExtractor) {
        updateTypesIfNecessary();
        return this.beanTypes.entrySet().stream().filter((entry) -> {
            Class<?> beanType = extractType(entry.getValue(), typeExtractor);
            return beanType != null && type.isAssignableFrom(beanType);
        }).map(Map.Entry::getKey).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Class<?> extractType(ResolvableType type, TypeExtractor extractor) {
        return (type != null) ? extractor.getBeanType(type) : null;
    }

    public Set<String> getNamesForAnnotation(Class<? extends Annotation> annotation) {
        updateTypesIfNecessary();
        return this.beanTypes.entrySet().stream().filter((entry) -> entry.getValue() != null && MergedAnnotations.from(entry.getValue().resolve(), MergedAnnotations.SearchStrategy.EXHAUSTIVE).isPresent(annotation)).map(Map.Entry::getKey).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public void afterSingletonsInstantiated() {
        // We're done at this point, free up some memory
        this.beanTypes.clear();
        this.beanDefinitions.clear();
    }

    private void updateTypesIfNecessary() {
        this.beanFactory.getBeanNamesIterator().forEachRemaining(this::updateTypesIfNecessary);
    }

    private void updateTypesIfNecessary(String name) {
        if (!this.beanTypes.containsKey(name)) {
            addBeanType(name);
        } else {
            updateBeanType(name);
        }
    }

    private void addBeanType(String name) {
        if (this.beanFactory.containsSingleton(name)) {
            this.beanTypes.put(name, getType(name, null));
        } else if (!this.beanFactory.isAlias(name)) {
            addBeanTypeForNonAliasDefinition(name);
        }
    }

    private void addBeanTypeForNonAliasDefinition(String name) {
        RootBeanDefinition definition = getBeanDefinition(name);
        if (definition != null) {
            addBeanTypeForNonAliasDefinition(name, definition);
        }
    }

    private void updateBeanType(String name) {
        if (this.beanFactory.isAlias(name) || this.beanFactory.containsSingleton(name)) {
            return;
        }
        RootBeanDefinition definition = getBeanDefinition(name);
        if (definition == null) {
            return;
        }
        RootBeanDefinition previous = this.beanDefinitions.put(name, definition);
        if (previous != null && !definition.equals(previous)) {
            addBeanTypeForNonAliasDefinition(name, definition);
        }
    }

    private RootBeanDefinition getBeanDefinition(String name) {
        try {
            return (RootBeanDefinition) this.beanFactory.getMergedBeanDefinition(name);
        } catch (BeanDefinitionStoreException ex) {
            logIgnoredError("unresolvable metadata in bean definition", name, ex);
            return null;
        }
    }

    private void addBeanTypeForNonAliasDefinition(String name, RootBeanDefinition definition) {
        try {
            if (!definition.isAbstract() && !requiresEagerInit(definition.getFactoryBeanName())) {
                ResolvableType factoryMethodReturnType = getFactoryMethodReturnType(definition);
                String factoryBeanName = BeanFactory.FACTORY_BEAN_PREFIX + name;
                if (this.beanFactory.isFactoryBean(factoryBeanName)) {
                    ResolvableType factoryBeanGeneric = getFactoryBeanGeneric(this.beanFactory, definition, factoryMethodReturnType);
                    this.beanTypes.put(name, factoryBeanGeneric);
                    this.beanTypes.put(factoryBeanName, getType(factoryBeanName, factoryMethodReturnType));
                } else {
                    this.beanTypes.put(name, getType(name, factoryMethodReturnType));
                }
            }
            this.beanDefinitions.put(name, definition);
        } catch (CannotLoadBeanClassException ex) {
            // Probably contains a placeholder
            logIgnoredError("bean class loading failure for bean", name, ex);
        }
    }

    private boolean requiresEagerInit(String factoryBeanName) {
        return (factoryBeanName != null && this.beanFactory.isFactoryBean(factoryBeanName) && !this.beanFactory.containsSingleton(factoryBeanName));
    }

    private ResolvableType getFactoryMethodReturnType(BeanDefinition definition) {
        try {
            if (StringUtils.hasLength(definition.getFactoryBeanName()) && StringUtils.hasLength(definition.getFactoryMethodName())) {
                Method method = getFactoryMethod(this.beanFactory, definition);
                ResolvableType type = (method != null) ? ResolvableType.forMethodReturnType(method) : null;
                return type;
            }
        } catch (Exception ex) {
        }
        return null;
    }

    private Method getFactoryMethod(ConfigurableListableBeanFactory beanFactory, BeanDefinition definition) throws Exception {
        if (definition instanceof AnnotatedBeanDefinition) {
            MethodMetadata factoryMethodMetadata = ((AnnotatedBeanDefinition) definition).getFactoryMethodMetadata();
            if (factoryMethodMetadata instanceof StandardMethodMetadata) {
                return ((StandardMethodMetadata) factoryMethodMetadata).getIntrospectedMethod();
            }
        }
        BeanDefinition factoryDefinition = beanFactory.getBeanDefinition(definition.getFactoryBeanName());
        Class<?> factoryClass = ClassUtils.forName(factoryDefinition.getBeanClassName(), beanFactory.getBeanClassLoader());
        return getFactoryMethod(definition, factoryClass);
    }

    private Method getFactoryMethod(BeanDefinition definition, Class<?> factoryClass) {
        Method uniqueMethod = null;
        for (Method candidate : getCandidateFactoryMethods(definition, factoryClass)) {
            if (candidate.getName().equals(definition.getFactoryMethodName())) {
                if (uniqueMethod == null) {
                    uniqueMethod = candidate;
                } else if (!hasMatchingParameterTypes(candidate, uniqueMethod)) {
                    return null;
                }
            }
        }
        return uniqueMethod;
    }

    private Method[] getCandidateFactoryMethods(BeanDefinition definition, Class<?> factoryClass) {
        return (shouldConsiderNonPublicMethods(definition) ? ReflectionUtils.getAllDeclaredMethods(factoryClass) : factoryClass.getMethods());
    }

    private boolean shouldConsiderNonPublicMethods(BeanDefinition definition) {
        return (definition instanceof AbstractBeanDefinition) && ((AbstractBeanDefinition) definition).isNonPublicAccessAllowed();
    }

    private boolean hasMatchingParameterTypes(Method candidate, Method current) {
        return Arrays.equals(candidate.getParameterTypes(), current.getParameterTypes());
    }

    private void logIgnoredError(String message, String name, Exception ex) {
        if (logger.isDebugEnabled()) {
            logger.debug("Ignoring " + message + " '" + name + "'", ex);
        }
    }

    private ResolvableType getFactoryBeanGeneric(ConfigurableListableBeanFactory beanFactory, BeanDefinition definition, ResolvableType factoryMethodReturnType) {
        try {
            if (factoryMethodReturnType != null) {
                return getFactoryBeanType(definition, factoryMethodReturnType);
            }
            if (StringUtils.hasLength(definition.getBeanClassName())) {
                return getDirectFactoryBeanGeneric(beanFactory, definition);
            }
        } catch (Exception ex) {
        }
        return null;
    }

    private ResolvableType getDirectFactoryBeanGeneric(ConfigurableListableBeanFactory beanFactory, BeanDefinition definition) throws ClassNotFoundException, LinkageError {
        Class<?> factoryBeanClass = ClassUtils.forName(definition.getBeanClassName(), beanFactory.getBeanClassLoader());
        return getFactoryBeanType(definition, ResolvableType.forClass(factoryBeanClass));
    }

    private ResolvableType getFactoryBeanType(BeanDefinition definition, ResolvableType type) throws ClassNotFoundException, LinkageError {
        ResolvableType generic = type.as(FactoryBean.class).getGeneric();
        if ((generic == null || generic.resolve().equals(Object.class)) && definition.hasAttribute(FACTORY_BEAN_OBJECT_TYPE)) {
            generic = getTypeFromAttribute(definition.getAttribute(FACTORY_BEAN_OBJECT_TYPE));
        }
        return generic;
    }

    private ResolvableType getTypeFromAttribute(Object attribute) throws ClassNotFoundException, LinkageError {
        if (attribute instanceof Class<?>) {
            return ResolvableType.forClass((Class<?>) attribute);
        }
        if (attribute instanceof String) {
            return ResolvableType.forClass(ClassUtils.forName((String) attribute, null));
        }
        return null;
    }

    private ResolvableType getType(String name, ResolvableType factoryMethodReturnType) {
        if (factoryMethodReturnType != null && !factoryMethodReturnType.resolve(Object.class).equals(Object.class)) {
            return factoryMethodReturnType;
        }
        Class<?> type = this.beanFactory.getType(name);
        return (type != null) ? ResolvableType.forClass(type) : null;
    }

    static BeanTypeRegistry get(ListableBeanFactory beanFactory) {
        Assert.isInstanceOf(DefaultListableBeanFactory.class, beanFactory);
        DefaultListableBeanFactory listableBeanFactory = (DefaultListableBeanFactory) beanFactory;
        Assert.isTrue(listableBeanFactory.isAllowEagerClassLoading(), "Bean factory must allow eager class loading");
        if (!listableBeanFactory.containsLocalBean(BEAN_NAME)) {
            BeanDefinition definition = BeanDefinitionBuilder.genericBeanDefinition(BeanTypeRegistry.class, () -> new BeanTypeRegistry((DefaultListableBeanFactory) beanFactory)).getBeanDefinition();
            listableBeanFactory.registerBeanDefinition(BEAN_NAME, definition);
        }
        return listableBeanFactory.getBean(BEAN_NAME, BeanTypeRegistry.class);
    }

    @FunctionalInterface
    interface TypeExtractor {

        Class<?> getBeanType(ResolvableType type);

    }

}
