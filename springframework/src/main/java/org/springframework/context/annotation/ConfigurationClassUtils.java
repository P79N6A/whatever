package org.springframework.context.annotation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.context.event.EventListenerFactory;
import org.springframework.core.Conventions;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

abstract class ConfigurationClassUtils {

    public static final String CONFIGURATION_CLASS_FULL = "full";

    public static final String CONFIGURATION_CLASS_LITE = "lite";

    /**
     * org.springframework.context.annotation.ConfigurationClassPostProcessor.configurationClass
     */
    public static final String CONFIGURATION_CLASS_ATTRIBUTE = Conventions.getQualifiedAttributeName(ConfigurationClassPostProcessor.class, "configurationClass");

    private static final String ORDER_ATTRIBUTE = Conventions.getQualifiedAttributeName(ConfigurationClassPostProcessor.class, "order");

    private static final Log logger = LogFactory.getLog(ConfigurationClassUtils.class);

    private static final Set<String> candidateIndicators = new HashSet<>(8);

    /*
     * 精简配置类包含的注解
     */
    static {
        // @Component注解也会当做配置类
        candidateIndicators.add(Component.class.getName());
        candidateIndicators.add(ComponentScan.class.getName());
        candidateIndicators.add(Import.class.getName());
        candidateIndicators.add(ImportResource.class.getName());
    }

    /**
     * 检查配置类候选
     * 类被@Configuration注解 || 有某些注解如@Component等
     */
    public static boolean checkConfigurationClassCandidate(BeanDefinition beanDef, MetadataReaderFactory metadataReaderFactory) {
        // Bean类名
        String className = beanDef.getBeanClassName();
        if (className == null || beanDef.getFactoryMethodName() != null) {
            return false;
        }
        AnnotationMetadata metadata;
        if (beanDef instanceof AnnotatedBeanDefinition && className.equals(((AnnotatedBeanDefinition) beanDef).getMetadata().getClassName())) {
            // Can reuse the pre-parsed metadata from the given BeanDefinition...
            metadata = ((AnnotatedBeanDefinition) beanDef).getMetadata();
        } else if (beanDef instanceof AbstractBeanDefinition && ((AbstractBeanDefinition) beanDef).hasBeanClass()) {
            // Check already loaded Class if present...
            // since we possibly can't even load the class file for this Class.
            Class<?> beanClass = ((AbstractBeanDefinition) beanDef).getBeanClass();
            if (BeanFactoryPostProcessor.class.isAssignableFrom(beanClass) || BeanPostProcessor.class.isAssignableFrom(beanClass) || AopInfrastructureBean.class.isAssignableFrom(beanClass) || EventListenerFactory.class.isAssignableFrom(beanClass)) {
                return false;
            }
            metadata = AnnotationMetadata.introspect(beanClass);
        } else {
            try {
                MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(className);
                metadata = metadataReader.getAnnotationMetadata();
            } catch (IOException ex) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Could not find class file for introspecting configuration annotations: " + className, ex);
                }
                return false;
            }
        }
        Map<String, Object> config = metadata.getAnnotationAttributes(Configuration.class.getName());
        // 类被@Configuration注解 && 代理的Bean方法（默认）
        if (config != null && !Boolean.FALSE.equals(config.get("proxyBeanMethods"))) {
            // 完整配置类
            beanDef.setAttribute(CONFIGURATION_CLASS_ATTRIBUTE, CONFIGURATION_CLASS_FULL);
        }
        // 类被@Configuration注解 || 有某些注解如@Component
        else if (config != null || isConfigurationCandidate(metadata)) {
            // 精简配置类
            beanDef.setAttribute(CONFIGURATION_CLASS_ATTRIBUTE, CONFIGURATION_CLASS_LITE);
        }
        //
        else {
            // 不是配置类
            return false;
        }
        // It's a full or lite configuration candidate... Let's determine the order value, if any.
        Integer order = getOrder(metadata);
        if (order != null) {
            beanDef.setAttribute(ORDER_ATTRIBUTE, order);
        }
        return true;
    }


    /**
     * 是否为配置类
     */
    public static boolean isConfigurationCandidate(AnnotationMetadata metadata) {
        // 接口或注解不是配置类
        if (metadata.isInterface()) {
            return false;
        }
        // @Component @ComponentScan @Import @ImportResource
        // 是配置类
        for (String indicator : candidateIndicators) {
            if (metadata.isAnnotated(indicator)) {
                return true;
            }
        }
        // 有@Bean注解的方法是配置类
        try {
            return metadata.hasAnnotatedMethods(Bean.class.getName());
        } catch (Throwable ex) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to introspect @Bean methods on class [" + metadata.getClassName() + "]: " + ex);
            }
            return false;
        }
    }

    @Nullable
    public static Integer getOrder(AnnotationMetadata metadata) {
        Map<String, Object> orderAttributes = metadata.getAnnotationAttributes(Order.class.getName());
        return (orderAttributes != null ? ((Integer) orderAttributes.get(AnnotationUtils.VALUE)) : null);
    }

    public static int getOrder(BeanDefinition beanDef) {
        Integer order = (Integer) beanDef.getAttribute(ORDER_ATTRIBUTE);
        return (order != null ? order : Ordered.LOWEST_PRECEDENCE);
    }

}
