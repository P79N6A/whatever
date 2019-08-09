package org.springframework.boot.context.properties;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.core.KotlinDetector;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class ConfigurationPropertiesBeanDefinitionRegistrar {

    private static final boolean KOTLIN_PRESENT = KotlinDetector.isKotlinPresent();

    private ConfigurationPropertiesBeanDefinitionRegistrar() {
    }

    public static void register(BeanDefinitionRegistry registry, ConfigurableListableBeanFactory beanFactory, Class<?> type) {
        String name = getName(type);
        // 没有注册
        if (!containsBeanDefinition(beanFactory, name)) {
            // 那就注册
            registerBeanDefinition(registry, beanFactory, name, type);
        }
    }

    private static String getName(Class<?> type) {
        // 获得类上的@ConfigurationProperties注解
        ConfigurationProperties annotation = AnnotationUtils.findAnnotation(type, ConfigurationProperties.class);
        String prefix = (annotation != null) ? annotation.prefix() : "";
        // 返回名称
        return (StringUtils.hasText(prefix) ? prefix + "-" + type.getName() : type.getName());
    }

    private static boolean containsBeanDefinition(ConfigurableListableBeanFactory beanFactory, String name) {
        if (beanFactory.containsBeanDefinition(name)) {
            return true;
        }
        BeanFactory parent = beanFactory.getParentBeanFactory();
        if (parent instanceof ConfigurableListableBeanFactory) {
            return containsBeanDefinition((ConfigurableListableBeanFactory) parent, name);
        }
        return false;
    }

    private static void registerBeanDefinition(BeanDefinitionRegistry registry, ConfigurableListableBeanFactory beanFactory, String name, Class<?> type) {
        assertHasAnnotation(type);
        registry.registerBeanDefinition(name, createBeanDefinition(beanFactory, name, type));
    }

    private static void assertHasAnnotation(Class<?> type) {
        Assert.isTrue(MergedAnnotations.from(type, SearchStrategy.EXHAUSTIVE).isPresent(ConfigurationProperties.class), () -> "No " + ConfigurationProperties.class.getSimpleName() + " annotation found on  '" + type.getName() + "'.");
    }

    private static BeanDefinition createBeanDefinition(ConfigurableListableBeanFactory beanFactory, String name, Class<?> type) {
        if (canBindAtCreationTime(type)) {
            return ConfigurationPropertiesBeanDefinition.from(beanFactory, name, type);
        }
        GenericBeanDefinition definition = new GenericBeanDefinition();
        definition.setBeanClass(type);
        return definition;
    }

    private static boolean canBindAtCreationTime(Class<?> type) {
        List<Constructor<?>> constructors = determineConstructors(type);
        return (constructors.size() == 1 && constructors.get(0).getParameterCount() > 0);
    }

    private static List<Constructor<?>> determineConstructors(Class<?> type) {
        List<Constructor<?>> constructors = new ArrayList<>();
        if (KOTLIN_PRESENT && KotlinDetector.isKotlinType(type)) {
            Constructor<?> primaryConstructor = BeanUtils.findPrimaryConstructor(type);
            if (primaryConstructor != null) {
                constructors.add(primaryConstructor);
            }
        } else {
            constructors.addAll(Arrays.asList(type.getDeclaredConstructors()));
        }
        return constructors;
    }

}
