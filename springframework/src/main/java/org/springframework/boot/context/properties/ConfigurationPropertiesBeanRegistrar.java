package org.springframework.boot.context.properties;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.MultiValueMap;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class ConfigurationPropertiesBeanRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        ConfigurableListableBeanFactory beanFactory = (ConfigurableListableBeanFactory) registry;
        // 把导入ConfigurationPropertiesBeanRegistrar自身的类，即@EnableConfigurationProperties的value包含的类注册到容器
        getTypes(metadata).forEach((type) -> ConfigurationPropertiesBeanDefinitionRegistrar.register(registry, beanFactory, type));
    }

    private List<Class<?>> getTypes(AnnotationMetadata metadata) {
        // 获取该@EnableConfigurationProperties的元数据
        MultiValueMap<String, Object> attributes = metadata.getAllAnnotationAttributes(EnableConfigurationProperties.class.getName(), false);
        // 获取@EnableConfigurationProperties注解的value
        return collectClasses((attributes != null) ? attributes.get("value") : Collections.emptyList());
    }

    private List<Class<?>> collectClasses(List<?> values) {
        // 遍历转型为Class数组并忽略void.class，collect成List返回
        return values.stream().flatMap((value) -> Arrays.stream((Class<?>[]) value)).filter((type) -> void.class != type).collect(Collectors.toList());
    }

}
