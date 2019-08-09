package org.springframework.context.annotation;

import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.lang.annotation.Annotation;
import java.util.function.Supplier;

public class AnnotatedBeanDefinitionReader {

    private final BeanDefinitionRegistry registry;

    private BeanNameGenerator beanNameGenerator = AnnotationBeanNameGenerator.INSTANCE;

    private ScopeMetadataResolver scopeMetadataResolver = new AnnotationScopeMetadataResolver();

    private ConditionEvaluator conditionEvaluator;

    public AnnotatedBeanDefinitionReader(BeanDefinitionRegistry registry) {
        this(registry, getOrCreateEnvironment(registry));
    }

    public AnnotatedBeanDefinitionReader(BeanDefinitionRegistry registry, Environment environment) {
        Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
        Assert.notNull(environment, "Environment must not be null");
        this.registry = registry;
        // BeanFactory、Environment、ResourceLoader、ClassLoader
        this.conditionEvaluator = new ConditionEvaluator(registry, environment, null);
        /*
         * 注册一些BeanPostProcessor的BeanDefinition到beanDefinitionMap
         *      org.springframework.context.annotation.ConfigurationClassPostProcessor 处理Configuration类
         *      org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor 处理@Autowired @Value @Inject
         *      org.springframework.context.annotation.CommonAnnotationBeanPostProcessor 处理JSR-250注解 @Resource
         *      org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor 处理JPA
         *      org.springframework.context.event.EventListenerMethodProcessor 处理@EventListener
         *      org.springframework.context.event.DefaultEventListenerFactory 处理EventListenerFactory
         */
        AnnotationConfigUtils.registerAnnotationConfigProcessors(this.registry);
    }

    public final BeanDefinitionRegistry getRegistry() {
        return this.registry;
    }

    public void setEnvironment(Environment environment) {
        this.conditionEvaluator = new ConditionEvaluator(this.registry, environment, null);
    }

    public void setBeanNameGenerator(@Nullable BeanNameGenerator beanNameGenerator) {
        this.beanNameGenerator = (beanNameGenerator != null ? beanNameGenerator : AnnotationBeanNameGenerator.INSTANCE);
    }

    public void setScopeMetadataResolver(@Nullable ScopeMetadataResolver scopeMetadataResolver) {
        this.scopeMetadataResolver = (scopeMetadataResolver != null ? scopeMetadataResolver : new AnnotationScopeMetadataResolver());
    }

    public void register(Class<?>... annotatedClasses) {
        for (Class<?> annotatedClass : annotatedClasses) {
            registerBean(annotatedClass);
        }
    }

    public void registerBean(Class<?> annotatedClass) {
        doRegisterBean(annotatedClass, null, null, null, null);
    }

    public void registerBean(Class<?> annotatedClass, @Nullable String name) {
        doRegisterBean(annotatedClass, name, null, null, null);
    }

    @SuppressWarnings("unchecked")
    public void registerBean(Class<?> annotatedClass, Class<? extends Annotation>... qualifiers) {
        doRegisterBean(annotatedClass, null, qualifiers, null, null);
    }

    @SuppressWarnings("unchecked")
    public void registerBean(Class<?> annotatedClass, @Nullable String name, Class<? extends Annotation>... qualifiers) {
        doRegisterBean(annotatedClass, name, qualifiers, null, null);
    }

    public <T> void registerBean(Class<T> annotatedClass, @Nullable Supplier<T> supplier) {
        doRegisterBean(annotatedClass, null, null, supplier, null);
    }

    public <T> void registerBean(Class<T> annotatedClass, @Nullable String name, @Nullable Supplier<T> supplier) {
        doRegisterBean(annotatedClass, name, null, supplier, null);
    }

    public <T> void registerBean(Class<T> annotatedClass, @Nullable String name, @Nullable Supplier<T> supplier, BeanDefinitionCustomizer... customizers) {
        doRegisterBean(annotatedClass, name, null, supplier, customizers);
    }

    private <T> void doRegisterBean(Class<T> annotatedClass, @Nullable String name, @Nullable Class<? extends Annotation>[] qualifiers, @Nullable Supplier<T> supplier, @Nullable BeanDefinitionCustomizer[] customizers) {
        // 创建BeanDefinition对象
        AnnotatedGenericBeanDefinition abd = new AnnotatedGenericBeanDefinition(annotatedClass);
        // 处理@Condition注解
        if (this.conditionEvaluator.shouldSkip(abd.getMetadata())) {
            return;
        }
        abd.setInstanceSupplier(supplier);
        ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(abd);
        // 设置单例还是多例，默认单例
        abd.setScope(scopeMetadata.getScopeName());
        // 获取beanName，设置的话就采用设置值，否则类名第一个字母小写
        String beanName = (name != null ? name : this.beanNameGenerator.generateBeanName(abd, this.registry));
        // 处理@Lazy，@Primary，@Primary，@Role，@Description注解
        AnnotationConfigUtils.processCommonDefinitionAnnotations(abd);
        if (qualifiers != null) {
            for (Class<? extends Annotation> qualifier : qualifiers) {
                if (Primary.class == qualifier) {
                    abd.setPrimary(true);
                } else if (Lazy.class == qualifier) {
                    abd.setLazyInit(true);
                } else {
                    abd.addQualifier(new AutowireCandidateQualifier(qualifier));
                }
            }
        }
        if (customizers != null) {
            for (BeanDefinitionCustomizer customizer : customizers) {
                customizer.customize(abd);
            }
        }
        // BeanDefinitionHolder
        BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(abd, beanName);
        // 判断对象需不需要代理，不需要直接返回，需要的重新创建BeanDefinition加入代理的信息
        definitionHolder = AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);
        // 注册配置类的BeanDefinition到beanDefinitionMap
        BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, this.registry);
    }

    private static Environment getOrCreateEnvironment(BeanDefinitionRegistry registry) {
        Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
        if (registry instanceof EnvironmentCapable) {
            return ((EnvironmentCapable) registry).getEnvironment();
        }
        return new StandardEnvironment();
    }

}
