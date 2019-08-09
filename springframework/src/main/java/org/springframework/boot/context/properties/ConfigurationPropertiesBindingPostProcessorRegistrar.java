package org.springframework.boot.context.properties;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

public class ConfigurationPropertiesBindingPostProcessorRegistrar implements ImportBeanDefinitionRegistrar {

    public static final String VALIDATOR_BEAN_NAME = "configurationPropertiesValidator";

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        if (!registry.containsBeanDefinition(ConfigurationPropertiesBinder.BEAN_NAME)) {
            // 注册ConfigurationPropertiesBinder的BeanDefinition，具体注入交给它
            registerConfigurationPropertiesBinder(registry);
        }
        if (!registry.containsBeanDefinition(ConfigurationPropertiesBindingPostProcessor.BEAN_NAME)) {
            // 注册ConfigurationPropertiesBindingPostProcessor的BeanDefinition
            registerConfigurationPropertiesBindingPostProcessor(registry);
            // 注册ConfigurationBeanFactoryMetadata的BeanDefinition
            registerConfigurationBeanFactoryMetadata(registry);
        }
    }

    private void registerConfigurationPropertiesBinder(BeanDefinitionRegistry registry) {
        GenericBeanDefinition definition = new GenericBeanDefinition();
        definition.setBeanClass(ConfigurationPropertiesBinder.class);
        definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        definition.getConstructorArgumentValues().addIndexedArgumentValue(0, VALIDATOR_BEAN_NAME);
        registry.registerBeanDefinition(ConfigurationPropertiesBinder.BEAN_NAME, definition);
    }

    private void registerConfigurationPropertiesBindingPostProcessor(BeanDefinitionRegistry registry) {
        GenericBeanDefinition definition = new GenericBeanDefinition();
        definition.setBeanClass(ConfigurationPropertiesBindingPostProcessor.class);
        // 表面该Bean是基础设施，不是用户定义的
        definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        // 注册
        registry.registerBeanDefinition(ConfigurationPropertiesBindingPostProcessor.BEAN_NAME, definition);
    }

    private void registerConfigurationBeanFactoryMetadata(BeanDefinitionRegistry registry) {
        GenericBeanDefinition definition = new GenericBeanDefinition();
        definition.setBeanClass(ConfigurationBeanFactoryMetadata.class);
        definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        registry.registerBeanDefinition(ConfigurationBeanFactoryMetadata.BEAN_NAME, definition);
    }

}
