package org.springframework.boot.context.properties;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

class ConfigurationPropertiesScanRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware, ResourceLoaderAware {

    private Environment environment;

    private ResourceLoader resourceLoader;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        // 从注解获取要扫描的包路径，空则默认注解类的包路径
        Set<String> packagesToScan = getPackagesToScan(importingClassMetadata);
        // 扫描并注册BeanDefinition到容器
        register(registry, (ConfigurableListableBeanFactory) registry, packagesToScan);
    }

    private Set<String> getPackagesToScan(AnnotationMetadata metadata) {
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(ConfigurationPropertiesScan.class.getName()));
        // 指定的包路径
        String[] basePackages = attributes.getStringArray("basePackages");
        Class<?>[] basePackageClasses = attributes.getClassArray("basePackageClasses");
        Set<String> packagesToScan = new LinkedHashSet<>(Arrays.asList(basePackages));
        for (Class<?> basePackageClass : basePackageClasses) {
            // 指定的类的路径
            packagesToScan.add(ClassUtils.getPackageName(basePackageClass));
        }
        if (packagesToScan.isEmpty()) {
            // 空的就加注解类的路径
            packagesToScan.add(ClassUtils.getPackageName(metadata.getClassName()));
        }
        return packagesToScan;
    }

    protected void register(BeanDefinitionRegistry registry, ConfigurableListableBeanFactory beanFactory, Set<String> packagesToScan) {
        scan(packagesToScan, beanFactory, registry);
    }

    protected void scan(Set<String> packages, ConfigurableListableBeanFactory beanFactory, BeanDefinitionRegistry registry) {
        /*
         * ClassPathBeanDefinitionScanner：扫描并注册Bean
         * ClassPathScanningCandidateComponentProvider：包扫描
         * ClassPathBeanDefinitionScanner继承了ClassPathScanningCandidateComponentProvider
         * 默认Filter是扫描@Component、JSR-250和JSR-330，这里只扫描@ConfigurationProperties
         */
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.setEnvironment(this.environment);
        scanner.setResourceLoader(this.resourceLoader);
        // 过滤器，扫描有@ConfigurationProperties注解的类
        scanner.addIncludeFilter(new AnnotationTypeFilter(ConfigurationProperties.class));
        for (String basePackage : packages) {
            if (StringUtils.hasText(basePackage)) {
                // 执行扫描注册
                scan(beanFactory, registry, scanner, basePackage);
            }
        }
    }

    private void scan(ConfigurableListableBeanFactory beanFactory, BeanDefinitionRegistry registry, ClassPathScanningCandidateComponentProvider scanner, String basePackage) throws LinkageError {
        // 扫描获得BeanDefinition
        for (BeanDefinition candidate : scanner.findCandidateComponents(basePackage)) {
            String beanClassName = candidate.getBeanClassName();
            try {
                Class<?> type = ClassUtils.forName(beanClassName, null);
                // 忽略有@Component，大概是重复扫描的问题
                validateScanConfiguration(type);
                // 注册被@ConfigurationProperties注解的类BeanDefinition到容器，具体注入交给ConfigurationPropertiesBindingPostProcessor处理
                ConfigurationPropertiesBeanDefinitionRegistrar.register(registry, beanFactory, type);
            } catch (ClassNotFoundException ex) {
                // Ignore
            }
        }
    }

    private void validateScanConfiguration(Class<?> type) {
        MergedAnnotation<Component> component = MergedAnnotations.from(type, MergedAnnotations.SearchStrategy.EXHAUSTIVE).get(Component.class);
        if (component.isPresent()) {
            throw new InvalidConfigurationPropertiesException(type, component.getRoot().getType());
        }
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

}
