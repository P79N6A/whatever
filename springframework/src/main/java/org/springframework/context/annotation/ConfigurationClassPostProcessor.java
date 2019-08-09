package org.springframework.context.annotation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.framework.autoproxy.AutoProxyUtils;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.*;
import org.springframework.beans.factory.parsing.FailFastProblemReporter;
import org.springframework.beans.factory.parsing.PassThroughSourceExtractor;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.beans.factory.parsing.SourceExtractor;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ConfigurationClassEnhancer.EnhancedConfiguration;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.*;

public class ConfigurationClassPostProcessor implements BeanDefinitionRegistryPostProcessor, PriorityOrdered, ResourceLoaderAware, BeanClassLoaderAware, EnvironmentAware {

    public static final AnnotationBeanNameGenerator IMPORT_BEAN_NAME_GENERATOR = new AnnotationBeanNameGenerator() {
        @Override
        protected String buildDefaultBeanName(BeanDefinition definition) {
            String beanClassName = definition.getBeanClassName();
            Assert.state(beanClassName != null, "No bean class name set");
            return beanClassName;
        }
    };

    private static final String IMPORT_REGISTRY_BEAN_NAME = ConfigurationClassPostProcessor.class.getName() + ".importRegistry";

    private final Log logger = LogFactory.getLog(getClass());

    private SourceExtractor sourceExtractor = new PassThroughSourceExtractor();

    private ProblemReporter problemReporter = new FailFastProblemReporter();

    @Nullable
    private Environment environment;

    private ResourceLoader resourceLoader = new DefaultResourceLoader();

    @Nullable
    private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

    private MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory();

    private boolean setMetadataReaderFactoryCalled = false;

    private final Set<Integer> registriesPostProcessed = new HashSet<>();

    private final Set<Integer> factoriesPostProcessed = new HashSet<>();

    @Nullable
    private ConfigurationClassBeanDefinitionReader reader;

    private boolean localBeanNameGeneratorSet = false;

    private BeanNameGenerator componentScanBeanNameGenerator = AnnotationBeanNameGenerator.INSTANCE;

    private BeanNameGenerator importBeanNameGenerator = IMPORT_BEAN_NAME_GENERATOR;

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;  // within PriorityOrdered
    }

    public void setSourceExtractor(@Nullable SourceExtractor sourceExtractor) {
        this.sourceExtractor = (sourceExtractor != null ? sourceExtractor : new PassThroughSourceExtractor());
    }

    public void setProblemReporter(@Nullable ProblemReporter problemReporter) {
        this.problemReporter = (problemReporter != null ? problemReporter : new FailFastProblemReporter());
    }

    public void setMetadataReaderFactory(MetadataReaderFactory metadataReaderFactory) {
        Assert.notNull(metadataReaderFactory, "MetadataReaderFactory must not be null");
        this.metadataReaderFactory = metadataReaderFactory;
        this.setMetadataReaderFactoryCalled = true;
    }

    public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
        Assert.notNull(beanNameGenerator, "BeanNameGenerator must not be null");
        this.localBeanNameGeneratorSet = true;
        this.componentScanBeanNameGenerator = beanNameGenerator;
        this.importBeanNameGenerator = beanNameGenerator;
    }

    @Override
    public void setEnvironment(Environment environment) {
        Assert.notNull(environment, "Environment must not be null");
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        Assert.notNull(resourceLoader, "ResourceLoader must not be null");
        this.resourceLoader = resourceLoader;
        if (!this.setMetadataReaderFactoryCalled) {
            this.metadataReaderFactory = new CachingMetadataReaderFactory(resourceLoader);
        }
    }

    @Override
    public void setBeanClassLoader(ClassLoader beanClassLoader) {
        this.beanClassLoader = beanClassLoader;
        if (!this.setMetadataReaderFactoryCalled) {
            this.metadataReaderFactory = new CachingMetadataReaderFactory(beanClassLoader);
        }
    }

    /**
     * 处理BeanDefinitionRegistry
     */
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
        // 每个BeanDefinitionRegistry一个Id
        int registryId = System.identityHashCode(registry);
        // 已处理过该BeanDefinitionRegistry
        if (this.registriesPostProcessed.contains(registryId)) {
            // 抛异常
            throw new IllegalStateException("postProcessBeanDefinitionRegistry already called on this post-processor against " + registry);
        }
        // 已处理过该BeanFactory
        if (this.factoriesPostProcessed.contains(registryId)) {
            // 抛异常
            throw new IllegalStateException("postProcessBeanFactory already called on this post-processor against " + registry);
        }
        // 添加进已处理BeanDefinitionRegistry
        this.registriesPostProcessed.add(registryId);
        // 处理该BeanDefinitionRegistry
        processConfigBeanDefinitions(registry);
    }

    /**
     * 处理BeanFactory
     */
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        // 一个BeanFactory一个Id
        int factoryId = System.identityHashCode(beanFactory);
        // 已处理过该BeanFactory
        if (this.factoriesPostProcessed.contains(factoryId)) {
            // 抛异常
            throw new IllegalStateException("postProcessBeanFactory already called on this post-processor against " + beanFactory);
        }
        // 添加进已处理BeanFactory
        this.factoriesPostProcessed.add(factoryId);
        // 如果没处理过该BeanDefinitionRegistry
        if (!this.registriesPostProcessed.contains(factoryId)) {
            // BeanDefinitionRegistryPostProcessor hook apparently not supported...
            // Simply call processConfigurationClasses lazily at this point then.
            processConfigBeanDefinitions((BeanDefinitionRegistry) beanFactory);
        }
        // 类增强，代理
        enhanceConfigurationClasses(beanFactory);
        // 将被@Import导入的Bean注入ImportAware实现
        beanFactory.addBeanPostProcessor(new ImportAwareBeanPostProcessor(beanFactory));
    }

    /**
     * 处理配置类BeanDefinition
     */
    public void processConfigBeanDefinitions(BeanDefinitionRegistry registry) {
        List<BeanDefinitionHolder> configCandidates = new ArrayList<>();
        // 获取已注册的BeanDefinition名称
        String[] candidateNames = registry.getBeanDefinitionNames();
        // 遍历
        for (String beanName : candidateNames) {
            // 获取BeanDefinition
            BeanDefinition beanDef = registry.getBeanDefinition(beanName);
            // 已标记处理
            if (beanDef.getAttribute(ConfigurationClassUtils.CONFIGURATION_CLASS_ATTRIBUTE) != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Bean definition has already been processed as a configuration class: " + beanDef);
                }
            }
            // 没处理，并且是配置类（有@Configuration，@Component，@ComponentScan，@Import，@ImportResource之一），打上标记
            else if (ConfigurationClassUtils.checkConfigurationClassCandidate(beanDef, this.metadataReaderFactory)) {
                // 放进去
                configCandidates.add(new BeanDefinitionHolder(beanDef, beanName));
            }
        }
        // 没有配置类，直接返回
        if (configCandidates.isEmpty()) {
            return;
        }
        // Sort by previously determined @Order value, if applicable
        configCandidates.sort((bd1, bd2) -> {
            int i1 = ConfigurationClassUtils.getOrder(bd1.getBeanDefinition());
            int i2 = ConfigurationClassUtils.getOrder(bd2.getBeanDefinition());
            return Integer.compare(i1, i2);
        });
        // Detect any custom bean name generation strategy supplied through the enclosing application context
        SingletonBeanRegistry sbr = null;
        if (registry instanceof SingletonBeanRegistry) {
            sbr = (SingletonBeanRegistry) registry;
            if (!this.localBeanNameGeneratorSet) {
                BeanNameGenerator generator = (BeanNameGenerator) sbr.getSingleton(AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR);
                if (generator != null) {
                    this.componentScanBeanNameGenerator = generator;
                    this.importBeanNameGenerator = generator;
                }
            }
        }
        if (this.environment == null) {
            this.environment = new StandardEnvironment();
        }
        // 解析工具
        ConfigurationClassParser parser = new ConfigurationClassParser(this.metadataReaderFactory, this.problemReporter, this.environment, this.resourceLoader, this.componentScanBeanNameGenerator, registry);
        Set<BeanDefinitionHolder> candidates = new LinkedHashSet<>(configCandidates);
        Set<ConfigurationClass> alreadyParsed = new HashSet<>(configCandidates.size());
        do {
            // 解析配置类
            parser.parse(candidates);
            parser.validate();
            // 配置类解析结果（ConfigurationClass集合）
            Set<ConfigurationClass> configClasses = new LinkedHashSet<>(parser.getConfigurationClasses());
            configClasses.removeAll(alreadyParsed);
            // Read the model and create bean definitions based on its content
            if (this.reader == null) {
                this.reader = new ConfigurationClassBeanDefinitionReader(registry, this.sourceExtractor, this.resourceLoader, this.environment, this.importBeanNameGenerator, parser.getImportRegistry());
            }
            // ConfigurationClassBeanDefinitionReader#loadBeanDefinitions
            // 从配置类的解析结果加载BeanDefinition
            this.reader.loadBeanDefinitions(configClasses);
            alreadyParsed.addAll(configClasses);
            // 清空
            candidates.clear();
            // 在上面解析的过程中加载了新的BeanDefinition
            if (registry.getBeanDefinitionCount() > candidateNames.length) {
                String[] newCandidateNames = registry.getBeanDefinitionNames();
                // 旧的BeanDefinition名称
                Set<String> oldCandidateNames = new HashSet<>(Arrays.asList(candidateNames));
                Set<String> alreadyParsedClasses = new HashSet<>();
                // 遍历已解析的配置类
                for (ConfigurationClass configurationClass : alreadyParsed) {
                    // 添加名称
                    alreadyParsedClasses.add(configurationClass.getMetadata().getClassName());
                }
                // 遍历新的BeanDefinition列表
                for (String candidateName : newCandidateNames) {
                    // 没包含，是新加的
                    if (!oldCandidateNames.contains(candidateName)) {
                        BeanDefinition bd = registry.getBeanDefinition(candidateName);
                        // 如果是配置类 && 没解析过
                        if (ConfigurationClassUtils.checkConfigurationClassCandidate(bd, this.metadataReaderFactory) && !alreadyParsedClasses.contains(bd.getBeanClassName())) {
                            // 加进去，继续解析
                            candidates.add(new BeanDefinitionHolder(bd, candidateName));
                        }
                    }
                }
                candidateNames = newCandidateNames;
            }
        } while (!candidates.isEmpty());
        // Register the ImportRegistry as a bean in order to support ImportAware @Configuration classes
        if (sbr != null && !sbr.containsSingleton(IMPORT_REGISTRY_BEAN_NAME)) {
            sbr.registerSingleton(IMPORT_REGISTRY_BEAN_NAME, parser.getImportRegistry());
        }
        if (this.metadataReaderFactory instanceof CachingMetadataReaderFactory) {
            // Clear cache in externally provided MetadataReaderFactory; this is a no-op
            // for a shared cache since it'll be cleared by the ApplicationContext.
            ((CachingMetadataReaderFactory) this.metadataReaderFactory).clearCache();
        }
    }

    /**
     * 为配置类进行CGLIB代理
     */
    public void enhanceConfigurationClasses(ConfigurableListableBeanFactory beanFactory) {
        Map<String, AbstractBeanDefinition> configBeanDefs = new LinkedHashMap<>();
        // 遍历
        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            BeanDefinition beanDef = beanFactory.getBeanDefinition(beanName);
            Object configClassAttr = beanDef.getAttribute(ConfigurationClassUtils.CONFIGURATION_CLASS_ATTRIBUTE);
            MethodMetadata methodMetadata = null;
            if (beanDef instanceof AnnotatedBeanDefinition) {
                methodMetadata = ((AnnotatedBeanDefinition) beanDef).getFactoryMethodMetadata();
            }
            if ((configClassAttr != null || methodMetadata != null) && beanDef instanceof AbstractBeanDefinition) {
                // Configuration class (full or lite) or a configuration-derived @Bean method
                // -> resolve bean class at this point...
                AbstractBeanDefinition abd = (AbstractBeanDefinition) beanDef;
                if (!abd.hasBeanClass()) {
                    try {
                        abd.resolveBeanClass(this.beanClassLoader);
                    } catch (Throwable ex) {
                        throw new IllegalStateException("Cannot load configuration class: " + beanDef.getBeanClassName(), ex);
                    }
                }
            }

            /*
             * CONFIGURATION_CLASS_FULL的是被@Configuration注解的类（默认proxyBeanMethods==true）
             * 调用被@Component注解的类对象里边被@Bean注解的方法，返回的是原始的对象，每次都是新的
             * 调用被@Configuration注解的类对象里边被@Bean注解的方法，返回的是代理对象，由容器管理，默认单例
             * 参考这篇文章：https://my.oschina.net/guangshan/blog/1807721
             */
            if (ConfigurationClassUtils.CONFIGURATION_CLASS_FULL.equals(configClassAttr)) {
                if (!(beanDef instanceof AbstractBeanDefinition)) {
                    throw new BeanDefinitionStoreException("Cannot enhance @Configuration bean definition '" + beanName + "' since it is not stored in an AbstractBeanDefinition subclass");
                } else if (logger.isInfoEnabled() && beanFactory.containsSingleton(beanName)) {
                    logger.info("Cannot enhance @Configuration bean definition '" + beanName + "' since its singleton instance has been created too early. The typical cause " + "is a non-static @Bean method with a BeanDefinitionRegistryPostProcessor " + "return type: Consider declaring such methods as 'static'.");
                }
                configBeanDefs.put(beanName, (AbstractBeanDefinition) beanDef);
            }
        }
        if (configBeanDefs.isEmpty()) {
            // nothing to enhance -> return immediately
            return;
        }
        ConfigurationClassEnhancer enhancer = new ConfigurationClassEnhancer();
        for (Map.Entry<String, AbstractBeanDefinition> entry : configBeanDefs.entrySet()) {
            AbstractBeanDefinition beanDef = entry.getValue();
            // 如果@Configuration类被代理了, 代理目标类
            beanDef.setAttribute(AutoProxyUtils.PRESERVE_TARGET_CLASS_ATTRIBUTE, Boolean.TRUE);
            // Set enhanced subclass of the user-specified bean class
            Class<?> configClass = beanDef.getBeanClass();
            // 类增强
            Class<?> enhancedClass = enhancer.enhance(configClass, this.beanClassLoader);
            if (configClass != enhancedClass) {
                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("Replacing bean definition '%s' existing class '%s' with " + "enhanced class '%s'", entry.getKey(), configClass.getName(), enhancedClass.getName()));
                }
                // 替换class为CGLIB代理的class
                beanDef.setBeanClass(enhancedClass);
            }
        }
    }

    private static class ImportAwareBeanPostProcessor extends InstantiationAwareBeanPostProcessorAdapter {

        private final BeanFactory beanFactory;

        public ImportAwareBeanPostProcessor(BeanFactory beanFactory) {
            this.beanFactory = beanFactory;
        }

        @Override
        public PropertyValues postProcessProperties(@Nullable PropertyValues pvs, Object bean, String beanName) {
            // Inject the BeanFactory before AutowiredAnnotationBeanPostProcessor's
            // postProcessProperties method attempts to autowire other configuration beans.
            if (bean instanceof EnhancedConfiguration) {
                ((EnhancedConfiguration) bean).setBeanFactory(this.beanFactory);
            }
            return pvs;
        }

        @Override
        public Object postProcessBeforeInitialization(Object bean, String beanName) {
            if (bean instanceof ImportAware) {
                ImportRegistry ir = this.beanFactory.getBean(IMPORT_REGISTRY_BEAN_NAME, ImportRegistry.class);
                // 之前被@Import导入的
                AnnotationMetadata importingClass = ir.getImportingClassFor(ClassUtils.getUserClass(bean).getName());
                if (importingClass != null) {
                    // 交给ImportAware的实现
                    ((ImportAware) bean).setImportMetadata(importingClass);
                }
            }
            return bean;
        }

    }

}
