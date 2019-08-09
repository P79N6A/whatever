package org.springframework.beans.factory.support;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

public abstract class BeanDefinitionReaderUtils {

    public static final String GENERATED_BEAN_NAME_SEPARATOR = BeanFactoryUtils.GENERATED_BEAN_NAME_SEPARATOR;

    public static AbstractBeanDefinition createBeanDefinition(@Nullable String parentName, @Nullable String className, @Nullable ClassLoader classLoader) throws ClassNotFoundException {
        GenericBeanDefinition bd = new GenericBeanDefinition();
        bd.setParentName(parentName);
        if (className != null) {
            if (classLoader != null) {
                bd.setBeanClass(ClassUtils.forName(className, classLoader));
            } else {
                bd.setBeanClassName(className);
            }
        }
        return bd;
    }

    public static String generateBeanName(BeanDefinition beanDefinition, BeanDefinitionRegistry registry) throws BeanDefinitionStoreException {
        return generateBeanName(beanDefinition, registry, false);
    }

    public static String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry, boolean isInnerBean) throws BeanDefinitionStoreException {
        String generatedBeanName = definition.getBeanClassName();
        if (generatedBeanName == null) {
            if (definition.getParentName() != null) {
                generatedBeanName = definition.getParentName() + "$child";
            } else if (definition.getFactoryBeanName() != null) {
                generatedBeanName = definition.getFactoryBeanName() + "$created";
            }
        }
        if (!StringUtils.hasText(generatedBeanName)) {
            throw new BeanDefinitionStoreException("Unnamed bean definition specifies neither " + "'class' nor 'parent' nor 'factory-bean' - can't generate bean name");
        }
        String id = generatedBeanName;
        if (isInnerBean) {
            // Inner bean: generate identity hashcode suffix.
            id = generatedBeanName + GENERATED_BEAN_NAME_SEPARATOR + ObjectUtils.getIdentityHexString(definition);
        } else {
            // Top-level bean: use plain class name with unique suffix if necessary.
            return uniqueBeanName(generatedBeanName, registry);
        }
        return id;
    }

    public static String uniqueBeanName(String beanName, BeanDefinitionRegistry registry) {
        String id = beanName;
        int counter = -1;
        // Increase counter until the id is unique.
        while (counter == -1 || registry.containsBeanDefinition(id)) {
            counter++;
            id = beanName + GENERATED_BEAN_NAME_SEPARATOR + counter;
        }
        return id;
    }

    /**
     *
     */
    public static void registerBeanDefinition(BeanDefinitionHolder definitionHolder, BeanDefinitionRegistry registry) throws BeanDefinitionStoreException {
        // Register bean definition under primary name.
        String beanName = definitionHolder.getBeanName();
        // 注册这个Bean
        registry.registerBeanDefinition(beanName, definitionHolder.getBeanDefinition());
        // 如果有别名，注册别名
        String[] aliases = definitionHolder.getAliases();
        if (aliases != null) {
            for (String alias : aliases) {
                // alias -> beanName映射，获取的时候，通过alias获得beanName，再查找
                registry.registerAlias(beanName, alias);
            }
        }
    }

    public static String registerWithGeneratedName(AbstractBeanDefinition definition, BeanDefinitionRegistry registry) throws BeanDefinitionStoreException {
        String generatedName = generateBeanName(definition, registry, false);
        registry.registerBeanDefinition(generatedName, definition);
        return generatedName;
    }

}
