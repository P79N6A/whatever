package org.springframework.context.annotation;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.*;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.PatternMatchUtils;

import java.util.LinkedHashSet;
import java.util.Set;

public class ClassPathBeanDefinitionScanner extends ClassPathScanningCandidateComponentProvider {

    private final BeanDefinitionRegistry registry;

    private BeanDefinitionDefaults beanDefinitionDefaults = new BeanDefinitionDefaults();

    @Nullable
    private String[] autowireCandidatePatterns;

    private BeanNameGenerator beanNameGenerator = AnnotationBeanNameGenerator.INSTANCE;

    private ScopeMetadataResolver scopeMetadataResolver = new AnnotationScopeMetadataResolver();

    private boolean includeAnnotationConfig = true;

    public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry) {
        this(registry, true);
    }

    public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters) {
        this(registry, useDefaultFilters, getOrCreateEnvironment(registry));
    }

    public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters, Environment environment) {
        this(registry, useDefaultFilters, environment, (registry instanceof ResourceLoader ? (ResourceLoader) registry : null));
    }

    public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters, Environment environment, @Nullable ResourceLoader resourceLoader) {
        Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
        this.registry = registry;
        if (useDefaultFilters) {
            // 将@Component、JSR-250、JSR-330的AnnotationTypeFilter加入includeFilters列表
            // 默认只扫描@Component、JSR-250、JSR-330的标记的类
            registerDefaultFilters();
        }
        setEnvironment(environment);
        setResourceLoader(resourceLoader);
    }

    @Override
    public final BeanDefinitionRegistry getRegistry() {
        return this.registry;
    }

    public void setBeanDefinitionDefaults(@Nullable BeanDefinitionDefaults beanDefinitionDefaults) {
        this.beanDefinitionDefaults = (beanDefinitionDefaults != null ? beanDefinitionDefaults : new BeanDefinitionDefaults());
    }

    public BeanDefinitionDefaults getBeanDefinitionDefaults() {
        return this.beanDefinitionDefaults;
    }

    public void setAutowireCandidatePatterns(@Nullable String... autowireCandidatePatterns) {
        this.autowireCandidatePatterns = autowireCandidatePatterns;
    }

    public void setBeanNameGenerator(@Nullable BeanNameGenerator beanNameGenerator) {
        this.beanNameGenerator = (beanNameGenerator != null ? beanNameGenerator : AnnotationBeanNameGenerator.INSTANCE);
    }

    public void setScopeMetadataResolver(@Nullable ScopeMetadataResolver scopeMetadataResolver) {
        this.scopeMetadataResolver = (scopeMetadataResolver != null ? scopeMetadataResolver : new AnnotationScopeMetadataResolver());
    }

    public void setScopedProxyMode(ScopedProxyMode scopedProxyMode) {
        this.scopeMetadataResolver = new AnnotationScopeMetadataResolver(scopedProxyMode);
    }

    public void setIncludeAnnotationConfig(boolean includeAnnotationConfig) {
        this.includeAnnotationConfig = includeAnnotationConfig;
    }

    public int scan(String... basePackages) {
        int beanCountAtScanStart = this.registry.getBeanDefinitionCount();
        doScan(basePackages);
        // Register annotation config processors, if necessary.
        if (this.includeAnnotationConfig) {
            AnnotationConfigUtils.registerAnnotationConfigProcessors(this.registry);
        }
        return (this.registry.getBeanDefinitionCount() - beanCountAtScanStart);
    }

    /**
     * 扫描
     */
    protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
        Assert.notEmpty(basePackages, "At least one base package must be specified");
        Set<BeanDefinitionHolder> beanDefinitions = new LinkedHashSet<>();
        // 遍历路径
        for (String basePackage : basePackages) {
            // 扫描获得BeanDefinition
            Set<BeanDefinition> candidates = findCandidateComponents(basePackage);
            // 遍历
            for (BeanDefinition candidate : candidates) {
                ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(candidate);
                candidate.setScope(scopeMetadata.getScopeName());
                String beanName = this.beanNameGenerator.generateBeanName(candidate, this.registry);
                if (candidate instanceof AbstractBeanDefinition) {
                    postProcessBeanDefinition((AbstractBeanDefinition) candidate, beanName);
                }
                if (candidate instanceof AnnotatedBeanDefinition) {
                    AnnotationConfigUtils.processCommonDefinitionAnnotations((AnnotatedBeanDefinition) candidate);
                }
                // 还没被注册
                if (checkCandidate(beanName, candidate)) {
                    BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(candidate, beanName);
                    definitionHolder = AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);
                    beanDefinitions.add(definitionHolder);
                    // 注册BeanDefinition
                    registerBeanDefinition(definitionHolder, this.registry);
                }
            }
        }
        return beanDefinitions;
    }

    protected void postProcessBeanDefinition(AbstractBeanDefinition beanDefinition, String beanName) {
        beanDefinition.applyDefaults(this.beanDefinitionDefaults);
        if (this.autowireCandidatePatterns != null) {
            beanDefinition.setAutowireCandidate(PatternMatchUtils.simpleMatch(this.autowireCandidatePatterns, beanName));
        }
    }

    protected void registerBeanDefinition(BeanDefinitionHolder definitionHolder, BeanDefinitionRegistry registry) {
        BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, registry);
    }

    protected boolean checkCandidate(String beanName, BeanDefinition beanDefinition) throws IllegalStateException {
        if (!this.registry.containsBeanDefinition(beanName)) {
            return true;
        }
        BeanDefinition existingDef = this.registry.getBeanDefinition(beanName);
        BeanDefinition originatingDef = existingDef.getOriginatingBeanDefinition();
        if (originatingDef != null) {
            existingDef = originatingDef;
        }
        if (isCompatible(beanDefinition, existingDef)) {
            return false;
        }
        throw new ConflictingBeanDefinitionException("Annotation-specified bean name '" + beanName + "' for bean class [" + beanDefinition.getBeanClassName() + "] conflicts with existing, " + "non-compatible bean definition of same name and class [" + existingDef.getBeanClassName() + "]");
    }

    protected boolean isCompatible(BeanDefinition newDefinition, BeanDefinition existingDefinition) {
        return (!(existingDefinition instanceof ScannedGenericBeanDefinition) ||  // explicitly registered overriding bean
                (newDefinition.getSource() != null && newDefinition.getSource().equals(existingDefinition.getSource())) ||  // scanned same file twice
                newDefinition.equals(existingDefinition));  // scanned equivalent class twice
    }

    private static Environment getOrCreateEnvironment(BeanDefinitionRegistry registry) {
        Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
        if (registry instanceof EnvironmentCapable) {
            return ((EnvironmentCapable) registry).getEnvironment();
        }
        return new StandardEnvironment();
    }

}
