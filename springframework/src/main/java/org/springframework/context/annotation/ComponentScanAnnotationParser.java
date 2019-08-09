package org.springframework.context.annotation;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.*;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.regex.Pattern;
/**
 * 处理@ComponentScan
 */
class ComponentScanAnnotationParser {

    private final Environment environment;

    private final ResourceLoader resourceLoader;

    private final BeanNameGenerator beanNameGenerator;

    private final BeanDefinitionRegistry registry;

    public ComponentScanAnnotationParser(Environment environment, ResourceLoader resourceLoader, BeanNameGenerator beanNameGenerator, BeanDefinitionRegistry registry) {
        this.environment = environment;
        this.resourceLoader = resourceLoader;
        this.beanNameGenerator = beanNameGenerator;
        this.registry = registry;
    }

    /**
     * 解析
     */
    public Set<BeanDefinitionHolder> parse(AnnotationAttributes componentScan, final String declaringClass) {
        ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(this.registry, componentScan.getBoolean("useDefaultFilters"), this.environment, this.resourceLoader);
        Class<? extends BeanNameGenerator> generatorClass = componentScan.getClass("nameGenerator");
        boolean useInheritedGenerator = (BeanNameGenerator.class == generatorClass);
        scanner.setBeanNameGenerator(useInheritedGenerator ? this.beanNameGenerator : BeanUtils.instantiateClass(generatorClass));
        ScopedProxyMode scopedProxyMode = componentScan.getEnum("scopedProxy");
        if (scopedProxyMode != ScopedProxyMode.DEFAULT) {
            scanner.setScopedProxyMode(scopedProxyMode);
        } else {
            Class<? extends ScopeMetadataResolver> resolverClass = componentScan.getClass("scopeResolver");
            scanner.setScopeMetadataResolver(BeanUtils.instantiateClass(resolverClass));
        }
        scanner.setResourcePattern(componentScan.getString("resourcePattern"));
        // 包含过滤
        for (AnnotationAttributes filter : componentScan.getAnnotationArray("includeFilters")) {
            for (TypeFilter typeFilter : typeFiltersFor(filter)) {
                // 添加过滤器
                scanner.addIncludeFilter(typeFilter);
            }
        }
        // 排除过滤
        for (AnnotationAttributes filter : componentScan.getAnnotationArray("excludeFilters")) {
            for (TypeFilter typeFilter : typeFiltersFor(filter)) {
                // 添加过滤器
                scanner.addExcludeFilter(typeFilter);
            }
        }
        boolean lazyInit = componentScan.getBoolean("lazyInit");
        if (lazyInit) {
            scanner.getBeanDefinitionDefaults().setLazyInit(true);
        }
        Set<String> basePackages = new LinkedHashSet<>();
        // 扫描路径
        String[] basePackagesArray = componentScan.getStringArray("basePackages");
        for (String pkg : basePackagesArray) {
            String[] tokenized = StringUtils.tokenizeToStringArray(this.environment.resolvePlaceholders(pkg), ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
            Collections.addAll(basePackages, tokenized);
        }
        for (Class<?> clazz : componentScan.getClassArray("basePackageClasses")) {
            basePackages.add(ClassUtils.getPackageName(clazz));
        }
        if (basePackages.isEmpty()) {
            // 被@ComponentScan注解的类包名
            basePackages.add(ClassUtils.getPackageName(declaringClass));
        }
        // 不包括被@ComponentScan注解的类
        scanner.addExcludeFilter(new AbstractTypeHierarchyTraversingFilter(false, false) {
            @Override
            protected boolean matchClassName(String className) {
                return declaringClass.equals(className);
            }
        });
        // 扫描
        return scanner.doScan(StringUtils.toStringArray(basePackages));
    }

    private List<TypeFilter> typeFiltersFor(AnnotationAttributes filterAttributes) {
        List<TypeFilter> typeFilters = new ArrayList<>();
        FilterType filterType = filterAttributes.getEnum("type");
        for (Class<?> filterClass : filterAttributes.getClassArray("classes")) {
            switch (filterType) {
                case ANNOTATION:
                    Assert.isAssignable(Annotation.class, filterClass, "@ComponentScan ANNOTATION type filter requires an annotation type");
                    @SuppressWarnings("unchecked") Class<Annotation> annotationType = (Class<Annotation>) filterClass;
                    typeFilters.add(new AnnotationTypeFilter(annotationType));
                    break;
                case ASSIGNABLE_TYPE:
                    typeFilters.add(new AssignableTypeFilter(filterClass));
                    break;
                case CUSTOM:
                    Assert.isAssignable(TypeFilter.class, filterClass, "@ComponentScan CUSTOM type filter requires a TypeFilter implementation");
                    TypeFilter filter = BeanUtils.instantiateClass(filterClass, TypeFilter.class);
                    ParserStrategyUtils.invokeAwareMethods(filter, this.environment, this.resourceLoader, this.registry);
                    typeFilters.add(filter);
                    break;
                default:
                    throw new IllegalArgumentException("Filter type not supported with Class value: " + filterType);
            }
        }
        for (String expression : filterAttributes.getStringArray("pattern")) {
            switch (filterType) {
                case ASPECTJ:
                    typeFilters.add(new AspectJTypeFilter(expression, this.resourceLoader.getClassLoader()));
                    break;
                case REGEX:
                    typeFilters.add(new RegexPatternTypeFilter(Pattern.compile(expression)));
                    break;
                default:
                    throw new IllegalArgumentException("Filter type not supported with String pattern: " + filterType);
            }
        }
        return typeFilters;
    }

}
