package org.springframework.context.annotation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.Location;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase;
import org.springframework.context.annotation.DeferredImportSelector.Group;
import org.springframework.core.NestedIOException;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.*;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.DefaultPropertySourceFactory;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.UnknownHostException;
import java.util.*;

class ConfigurationClassParser {

    private static final PropertySourceFactory DEFAULT_PROPERTY_SOURCE_FACTORY = new DefaultPropertySourceFactory();

    private static final Comparator<DeferredImportSelectorHolder> DEFERRED_IMPORT_COMPARATOR = (o1, o2) -> AnnotationAwareOrderComparator.INSTANCE.compare(o1.getImportSelector(), o2.getImportSelector());

    private final Log logger = LogFactory.getLog(getClass());

    private final MetadataReaderFactory metadataReaderFactory;

    private final ProblemReporter problemReporter;

    private final Environment environment;

    private final ResourceLoader resourceLoader;

    private final BeanDefinitionRegistry registry;

    private final ComponentScanAnnotationParser componentScanParser;

    private final ConditionEvaluator conditionEvaluator;

    private final Map<ConfigurationClass, ConfigurationClass> configurationClasses = new LinkedHashMap<>();

    private final Map<String, ConfigurationClass> knownSuperclasses = new HashMap<>();

    private final List<String> propertySourceNames = new ArrayList<>();

    private final ImportStack importStack = new ImportStack();

    private final DeferredImportSelectorHandler deferredImportSelectorHandler = new DeferredImportSelectorHandler();

    private final SourceClass objectSourceClass = new SourceClass(Object.class);

    public ConfigurationClassParser(MetadataReaderFactory metadataReaderFactory, ProblemReporter problemReporter, Environment environment, ResourceLoader resourceLoader, BeanNameGenerator componentScanBeanNameGenerator, BeanDefinitionRegistry registry) {
        this.metadataReaderFactory = metadataReaderFactory;
        this.problemReporter = problemReporter;
        this.environment = environment;
        this.resourceLoader = resourceLoader;
        this.registry = registry;
        this.componentScanParser = new ComponentScanAnnotationParser(environment, resourceLoader, componentScanBeanNameGenerator, registry);
        this.conditionEvaluator = new ConditionEvaluator(registry, environment, resourceLoader);
    }


    public void parse(Set<BeanDefinitionHolder> configCandidates) {
        // 遍历
        for (BeanDefinitionHolder holder : configCandidates) {
            BeanDefinition bd = holder.getBeanDefinition();
            try {
                if (bd instanceof AnnotatedBeanDefinition) {
                    // 解析
                    parse(((AnnotatedBeanDefinition) bd).getMetadata(), holder.getBeanName());
                } else if (bd instanceof AbstractBeanDefinition && ((AbstractBeanDefinition) bd).hasBeanClass()) {
                    parse(((AbstractBeanDefinition) bd).getBeanClass(), holder.getBeanName());
                } else {
                    parse(bd.getBeanClassName(), holder.getBeanName());
                }
            } catch (BeanDefinitionStoreException ex) {
                throw ex;
            } catch (Throwable ex) {
                throw new BeanDefinitionStoreException("Failed to parse configuration class [" + bd.getBeanClassName() + "]", ex);
            }
        }
        // 处理延迟
        this.deferredImportSelectorHandler.process();
    }

    protected final void parse(@Nullable String className, String beanName) throws IOException {
        Assert.notNull(className, "No bean class name for configuration class bean definition");
        MetadataReader reader = this.metadataReaderFactory.getMetadataReader(className);
        processConfigurationClass(new ConfigurationClass(reader, beanName));
    }

    protected final void parse(Class<?> clazz, String beanName) throws IOException {
        processConfigurationClass(new ConfigurationClass(clazz, beanName));
    }

    protected final void parse(AnnotationMetadata metadata, String beanName) throws IOException {
        processConfigurationClass(new ConfigurationClass(metadata, beanName));
    }

    public void validate() {
        for (ConfigurationClass configClass : this.configurationClasses.keySet()) {
            configClass.validate(this.problemReporter);
        }
    }

    public Set<ConfigurationClass> getConfigurationClasses() {
        return this.configurationClasses.keySet();
    }

    /**
     * 解析Configuration类，放到configurationClasses列表
     */
    protected void processConfigurationClass(ConfigurationClass configClass) throws IOException {
        // 处理@Condition
        if (this.conditionEvaluator.shouldSkip(configClass.getMetadata(), ConfigurationPhase.PARSE_CONFIGURATION)) {
            return;
        }
        ConfigurationClass existingClass = this.configurationClasses.get(configClass);
        // 已存在，重复
        if (existingClass != null) {
            // 新的配置类是被导入的
            if (configClass.isImported()) {
                // 旧的配置类也是被导入的
                if (existingClass.isImported()) {
                    // 合并新的导入，覆盖旧的
                    existingClass.mergeImportedBy(configClass);
                }
                // 忽略新导入的配置类，现存的非被导入的覆盖它
                return;
            } else {
                // 移除旧的，用新的替换
                this.configurationClasses.remove(configClass);
                this.knownSuperclasses.values().removeIf(configClass::equals);
            }
        }
        // 递归处理配置类和其父类，得到SourceClass对象
        SourceClass sourceClass = asSourceClass(configClass);
        do {
            // 解析
            sourceClass = doProcessConfigurationClass(configClass, sourceClass);
        } while (sourceClass != null);
        // 解析完放入
        this.configurationClasses.put(configClass, configClass);
    }

    /**
     * 一堆处理
     */
    @Nullable
    protected final SourceClass doProcessConfigurationClass(ConfigurationClass configClass, SourceClass sourceClass) throws IOException {
        // 如果配置类被@Component注解
        if (configClass.getMetadata().isAnnotated(Component.class.getName())) {
            // 处理内部成员类，如果内部成员类也是配置类，递归处理processConfigurationClass
            processMemberClasses(configClass, sourceClass);
        }
        // 处理@PropertySource，@PropertySource("classpath:xxx.properties")，导入PropertySource到Environment
        for (AnnotationAttributes propertySource : AnnotationConfigUtils.attributesForRepeatable(sourceClass.getMetadata(), PropertySources.class, org.springframework.context.annotation.PropertySource.class)) {
            if (this.environment instanceof ConfigurableEnvironment) {
                processPropertySource(propertySource);
            } else {
                logger.info("Ignoring @PropertySource annotation on [" + sourceClass.getMetadata().getClassName() + "]. Reason: Environment must implement ConfigurableEnvironment");
            }
        }
        // 处理@ComponentScan
        Set<AnnotationAttributes> componentScans = AnnotationConfigUtils.attributesForRepeatable(sourceClass.getMetadata(), ComponentScans.class, ComponentScan.class);
        if (!componentScans.isEmpty() && !this.conditionEvaluator.shouldSkip(sourceClass.getMetadata(), ConfigurationPhase.REGISTER_BEAN)) {
            // 遍历
            for (AnnotationAttributes componentScan : componentScans) {
                // 如果配置类被@ComponentScan注解，马上扫描路径，并注册BeanDefinition，返回BeanDefinitionHolder集合
                Set<BeanDefinitionHolder> scannedBeanDefinitions = this.componentScanParser.parse(componentScan, sourceClass.getMetadata().getClassName());
                // 遍历扫描到的集合，看有没有配置类需要处理
                for (BeanDefinitionHolder holder : scannedBeanDefinitions) {
                    BeanDefinition bdCand = holder.getBeanDefinition().getOriginatingBeanDefinition();
                    if (bdCand == null) {
                        bdCand = holder.getBeanDefinition();
                    }
                    // 是配置类
                    if (ConfigurationClassUtils.checkConfigurationClassCandidate(bdCand, this.metadataReaderFactory)) {
                        // 递归解析
                        parse(bdCand.getBeanClassName(), holder.getBeanName());
                    }
                }
            }
        }
        // 处理@Import
        // @Import({XxxConfig.class, YyyConfig.class})，导入别的配置类的配置，合并到主配置
        // 也可以导入普通类，自动声明为一个Bean添加到容器，如@Import(DemoService.class)
        processImports(configClass, sourceClass, getImports(sourceClass), true);
        // 处理@ImportResource，@ImportResource("classpath:xxx.xml")，就是导入Xml的配置，合并到配置类
        AnnotationAttributes importResource = AnnotationConfigUtils.attributesFor(sourceClass.getMetadata(), ImportResource.class);
        if (importResource != null) {
            // 路径
            String[] resources = importResource.getStringArray("locations");
            // 默认BeanDefinitionReader
            Class<? extends BeanDefinitionReader> readerClass = importResource.getClass("reader");
            for (String resource : resources) {
                String resolvedResource = this.environment.resolveRequiredPlaceholders(resource);
                // 添加到configClass，等待ConfigurationClassBeanDefinitionReader#loadBeanDefinitionsForConfigurationClass处理
                configClass.addImportedResource(resolvedResource, readerClass);
            }
        }
        // 处理@Bean方法：将被@Bean注解的方法的元数据转化为BeanMethod添加到configClass
        Set<MethodMetadata> beanMethods = retrieveBeanMethodMetadata(sourceClass);
        for (MethodMetadata methodMetadata : beanMethods) {
            configClass.addBeanMethod(new BeanMethod(methodMetadata, configClass));
        }
        // 处理接口的默认方法：如果不是抽象方法，转化为BeanMethod添加到configClass
        processInterfaces(configClass, sourceClass);
        // 如果配置类有父类，处理
        if (sourceClass.getMetadata().hasSuperClass()) {
            String superclass = sourceClass.getMetadata().getSuperClassName();
            // 包名不是java开头 && 不在已知父类内
            if (superclass != null && !superclass.startsWith("java") && !this.knownSuperclasses.containsKey(superclass)) {
                // 放入已知父类
                this.knownSuperclasses.put(superclass, configClass);
                // 如果有父类，返回，递归
                return sourceClass.getSuperClass();
            }
        }
        // 没有父类，解析完成
        return null;
    }

    /**
     * 处理成员变量
     */
    private void processMemberClasses(ConfigurationClass configClass, SourceClass sourceClass) throws IOException {
        // 获取成员变量到元数据集合
        Collection<SourceClass> memberClasses = sourceClass.getMemberClasses();
        // 如果存在
        if (!memberClasses.isEmpty()) {
            // 候选集合
            List<SourceClass> candidates = new ArrayList<>(memberClasses.size());
            // 遍历
            for (SourceClass memberClass : memberClasses) {
                // 该成员是个配置类（@Component @ComponentScan @Import @ImportResource @Bean） && 成员类名!=配置类类名
                if (ConfigurationClassUtils.isConfigurationCandidate(memberClass.getMetadata()) && !memberClass.getMetadata().getClassName().equals(configClass.getMetadata().getClassName())) {
                    // 添加到候选
                    candidates.add(memberClass);
                }
            }
            // 排序
            OrderComparator.sort(candidates);
            // 遍历候选
            for (SourceClass candidate : candidates) {
                // 已经有
                if (this.importStack.contains(configClass)) {
                    // 循环导入异常
                    this.problemReporter.error(new CircularImportProblem(configClass, this.importStack));
                } else {
                    // 入栈
                    this.importStack.push(configClass);
                    try {
                        // 处理成员类
                        processConfigurationClass(candidate.asConfigClass(configClass));
                    } finally {
                        // 出栈
                        this.importStack.pop();
                    }
                }
            }
        }
    }

    /**
     * 处理接口
     */
    private void processInterfaces(ConfigurationClass configClass, SourceClass sourceClass) throws IOException {
        // 遍历接口
        for (SourceClass ifc : sourceClass.getInterfaces()) {
            // 接口方法元数据集合
            Set<MethodMetadata> beanMethods = retrieveBeanMethodMetadata(ifc);
            // 遍历方法
            for (MethodMetadata methodMetadata : beanMethods) {
                // 不是抽象方法
                if (!methodMetadata.isAbstract()) {
                    // A default method or other concrete method on a Java 8+ interface...
                    configClass.addBeanMethod(new BeanMethod(methodMetadata, configClass));
                }
            }
            // 递归
            processInterfaces(configClass, ifc);
        }
    }

    /**
     * 获取被@Bean注解的方法的元数据
     */
    private Set<MethodMetadata> retrieveBeanMethodMetadata(SourceClass sourceClass) {
        AnnotationMetadata original = sourceClass.getMetadata();
        // 获取被@Bean注解的方法的元数据
        Set<MethodMetadata> beanMethods = original.getAnnotatedMethods(Bean.class.getName());
        // 如果存在被@Bean注解的方法 && xxx
        if (beanMethods.size() > 1 && original instanceof StandardAnnotationMetadata) {
            // Try reading the class file via ASM for deterministic declaration order...
            // Unfortunately, the JVM's standard reflection returns methods in arbitrary
            // order, even between different runs of the same application on the same JVM.
            try {
                AnnotationMetadata asm = this.metadataReaderFactory.getMetadataReader(original.getClassName()).getAnnotationMetadata();
                Set<MethodMetadata> asmMethods = asm.getAnnotatedMethods(Bean.class.getName());
                if (asmMethods.size() >= beanMethods.size()) {
                    Set<MethodMetadata> selectedMethods = new LinkedHashSet<>(asmMethods.size());
                    for (MethodMetadata asmMethod : asmMethods) {
                        for (MethodMetadata beanMethod : beanMethods) {
                            if (beanMethod.getMethodName().equals(asmMethod.getMethodName())) {
                                selectedMethods.add(beanMethod);
                                break;
                            }
                        }
                    }
                    if (selectedMethods.size() == beanMethods.size()) {
                        // All reflection-detected methods found in ASM method set -> proceed
                        beanMethods = selectedMethods;
                    }
                }
            } catch (IOException ex) {
                logger.debug("Failed to read class file via ASM for determining @Bean method order", ex);
                // No worries, let's continue with the reflection metadata we started with...
            }
        }
        return beanMethods;
    }

    /**
     * 处理@PropertySource注解
     */
    private void processPropertySource(AnnotationAttributes propertySource) throws IOException {
        String name = propertySource.getString("name");
        if (!StringUtils.hasLength(name)) {
            name = null;
        }
        String encoding = propertySource.getString("encoding");
        if (!StringUtils.hasLength(encoding)) {
            encoding = null;
        }
        // Resource路径
        String[] locations = propertySource.getStringArray("value");
        // 必填
        Assert.isTrue(locations.length > 0, "At least one @PropertySource(value) location is required");
        // 忽略未找到的Resource
        boolean ignoreResourceNotFound = propertySource.getBoolean("ignoreResourceNotFound");
        Class<? extends PropertySourceFactory> factoryClass = propertySource.getClass("factory");
        // 获取工厂
        PropertySourceFactory factory = (factoryClass == PropertySourceFactory.class ? DEFAULT_PROPERTY_SOURCE_FACTORY : BeanUtils.instantiateClass(factoryClass));
        // 遍历
        for (String location : locations) {
            try {
                String resolvedLocation = this.environment.resolveRequiredPlaceholders(location);
                // 获取Resource
                Resource resource = this.resourceLoader.getResource(resolvedLocation);
                // 添加PropertySource
                addPropertySource(factory.createPropertySource(name, new EncodedResource(resource, encoding)));
            } catch (IllegalArgumentException | FileNotFoundException | UnknownHostException ex) {
                // Placeholders not resolvable or resource not found when trying to open it
                if (ignoreResourceNotFound) {
                    if (logger.isInfoEnabled()) {
                        logger.info("Properties location [" + location + "] not resolvable: " + ex.getMessage());
                    }
                } else {
                    throw ex;
                }
            }
        }
    }

    /**
     * 添加PropertySource到Environment
     */
    private void addPropertySource(PropertySource<?> propertySource) {
        String name = propertySource.getName();
        // 全局的
        MutablePropertySources propertySources = ((ConfigurableEnvironment) this.environment).getPropertySources();
        //  已当前包含
        if (this.propertySourceNames.contains(name)) {
            // 全局已存在
            PropertySource<?> existing = propertySources.get(name);
            if (existing != null) {
                // 拓展
                PropertySource<?> newSource = (propertySource instanceof ResourcePropertySource ? ((ResourcePropertySource) propertySource).withResourceName() : propertySource);
                if (existing instanceof CompositePropertySource) {
                    ((CompositePropertySource) existing).addFirstPropertySource(newSource);
                } else {
                    if (existing instanceof ResourcePropertySource) {
                        existing = ((ResourcePropertySource) existing).withResourceName();
                    }
                    CompositePropertySource composite = new CompositePropertySource(name);
                    composite.addPropertySource(newSource);
                    composite.addPropertySource(existing);
                    propertySources.replace(name, composite);
                }
                return;
            }
        }
        // 加加加
        if (this.propertySourceNames.isEmpty()) {
            propertySources.addLast(propertySource);
        } else {
            String firstProcessed = this.propertySourceNames.get(this.propertySourceNames.size() - 1);
            propertySources.addBefore(firstProcessed, propertySource);
        }
        this.propertySourceNames.add(name);
    }

    private Set<SourceClass> getImports(SourceClass sourceClass) throws IOException {
        Set<SourceClass> imports = new LinkedHashSet<>();
        Set<SourceClass> visited = new LinkedHashSet<>();
        collectImports(sourceClass, imports, visited);
        return imports;
    }

    private void collectImports(SourceClass sourceClass, Set<SourceClass> imports, Set<SourceClass> visited) throws IOException {
        if (visited.add(sourceClass)) {
            for (SourceClass annotation : sourceClass.getAnnotations()) {
                String annName = annotation.getMetadata().getClassName();
                if (!annName.equals(Import.class.getName())) {
                    // 递归，包含注解配置类的注解里面的@Import，如@EnableAspectJAutoProxy
                    collectImports(annotation, imports, visited);
                }
            }
            imports.addAll(sourceClass.getAnnotationAttributes(Import.class.getName(), "value"));
        }
    }

    /**
     * @Import四种用法 ：
     * 导入一个Bean
     * 导入@Configuration注解的配置类
     * 导入ImportSelector的实现类
     * 导入ImportBeanDefinitionRegistrar的实现类
     */
    private void processImports(ConfigurationClass configClass, SourceClass currentSourceClass, Collection<SourceClass> importCandidates, boolean checkForCircularImports) {
        if (importCandidates.isEmpty()) {
            return;
        }
        // 处理之前被@Import导入的配置类
        if (checkForCircularImports && isChainedImportOnStack(configClass)) {
            this.problemReporter.error(new CircularImportProblem(configClass, this.importStack));
        } else {
            // 入栈
            this.importStack.push(configClass);
            try {
                /*
                 * 如果被import的是ImportSelector.class接口实现，初始化这个类，然后调用它的selectImports方法获得真正要导入的实现类，递归处理
                 * 如果被import的是ImportBeanDefinitionRegistrar.class接口实现，初始化，添加到importBeanDefinitionRegistrars映射，解析完后ConfigurationClassBeanDefinitionReader#loadBeanDefinitionsForConfigurationClass处理
                 * 否则将被import的类作为普通类或者配置类处理
                 */
                for (SourceClass candidate : importCandidates) {
                    /*
                     * 注解
                     * @Retention(RetentionPolicy.RUNTIME)
                     * @Documented
                     * @Target(ElementType.TYPE)
                     * @Import(XxxServiceImportSelector.class)
                     * public @interface EnableXxxService {
                     *     String name();
                     * }
                     *
                     * 实现类
                     * public class XxxServiceImportSelector implements ImportSelector {
                     *     @Override
                     *     public String[] selectImports(AnnotationMetadata importingClassMetadata) {
                     *        return new String[]{XxxServiceImpl.class.getName()};
                     *     }
                     * }
                     *
                     * 配置类
                     * @Configuration()
                     * @EnableXxxService(name="xxx")
                     * public class Config {
                     * }
                     */
                    if (candidate.isAssignable(ImportSelector.class)) {
                        // 加载类
                        Class<?> candidateClass = candidate.loadClass();
                        // 反射实例化
                        ImportSelector selector = BeanUtils.instantiateClass(candidateClass, ImportSelector.class);
                        // 如果该实现类还继承了Aware接口的话，手动注入对应资源
                        ParserStrategyUtils.invokeAwareMethods(selector, this.environment, this.resourceLoader, this.registry);
                        // 延迟实现
                        if (selector instanceof DeferredImportSelector) {
                            this.deferredImportSelectorHandler.handle(configClass, (DeferredImportSelector) selector);
                        }
                        // 普通实现
                        else {
                            // 调用ImportSelector实现类selectImports方法
                            String[] importClassNames = selector.selectImports(currentSourceClass.getMetadata());
                            // 需要真正导入的实现类
                            Collection<SourceClass> importSourceClasses = asSourceClasses(importClassNames);
                            // 递归处理
                            processImports(configClass, currentSourceClass, importSourceClasses, false);
                        }
                    }
                    /*
                     * 实现类
                     * public class XxxBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar {
                     *     @Override
                     *     public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
                     *         BeanDefinitionBuilder xxxService = BeanDefinitionBuilder.rootBeanDefinition(XxxServiceImpl.class);
                     *         // 通过BeanDefinitionRegistry注入到容器
                     *         registry.registerBeanDefinition("xxxService", xxxService.getBeanDefinition());
                     *     }
                     * }
                     *
                     * 配置类
                     * @Configuration(value="xxx")
                     * @Import(value={XxxBeanDefinitionRegistrar.class})
                     * public class Config {
                     * }
                     */
                    else if (candidate.isAssignable(ImportBeanDefinitionRegistrar.class)) {
                        // 加载ImportBeanDefinitionRegistrar实现类
                        Class<?> candidateClass = candidate.loadClass();
                        // 反射实例化
                        ImportBeanDefinitionRegistrar registrar = BeanUtils.instantiateClass(candidateClass, ImportBeanDefinitionRegistrar.class);
                        // 如果该实现类还继承了Aware接口的话，手动注入对应资源
                        ParserStrategyUtils.invokeAwareMethods(registrar, this.environment, this.resourceLoader, this.registry);
                        // 添加到importBeanDefinitionRegistrars映射，后续会由ConfigurationClassBeanDefinitionReader#loadBeanDefinitionsForConfigurationClass加载BeanDefinition
                        configClass.addImportBeanDefinitionRegistrar(registrar, currentSourceClass.getMetadata());
                    }
                    // 其它
                    else {
                        // 普通的Bean导入这里，添加待ImportAwareBeanPostProcessor后续处理
                        // 配置类元数据-导入Bean类名
                        this.importStack.registerImport(currentSourceClass.getMetadata(), candidate.getMetadata().getClassName());
                        // 配置类这里，递归处理
                        processConfigurationClass(candidate.asConfigClass(configClass));
                    }
                }
            } catch (BeanDefinitionStoreException ex) {
                throw ex;
            } catch (Throwable ex) {
                throw new BeanDefinitionStoreException("Failed to process import candidates for configuration class [" + configClass.getMetadata().getClassName() + "]", ex);
            } finally {
                // 出栈
                this.importStack.pop();
            }
        }
    }

    private boolean isChainedImportOnStack(ConfigurationClass configClass) {
        if (this.importStack.contains(configClass)) {
            String configClassName = configClass.getMetadata().getClassName();
            AnnotationMetadata importingClass = this.importStack.getImportingClassFor(configClassName);
            while (importingClass != null) {
                if (configClassName.equals(importingClass.getClassName())) {
                    return true;
                }
                importingClass = this.importStack.getImportingClassFor(importingClass.getClassName());
            }
        }
        return false;
    }

    ImportRegistry getImportRegistry() {
        return this.importStack;
    }

    private SourceClass asSourceClass(ConfigurationClass configurationClass) throws IOException {
        AnnotationMetadata metadata = configurationClass.getMetadata();
        if (metadata instanceof StandardAnnotationMetadata) {
            return asSourceClass(((StandardAnnotationMetadata) metadata).getIntrospectedClass());
        }
        return asSourceClass(metadata.getClassName());
    }

    SourceClass asSourceClass(@Nullable Class<?> classType) throws IOException {
        if (classType == null || classType.getName().startsWith("java.lang.annotation.")) {
            return this.objectSourceClass;
        }
        try {
            // Sanity test that we can reflectively read annotations,
            // including Class attributes; if not -> fall back to ASM
            for (Annotation ann : classType.getDeclaredAnnotations()) {
                AnnotationUtils.validateAnnotation(ann);
            }
            return new SourceClass(classType);
        } catch (Throwable ex) {
            // Enforce ASM via class name resolution
            return asSourceClass(classType.getName());
        }
    }

    private Collection<SourceClass> asSourceClasses(String... classNames) throws IOException {
        List<SourceClass> annotatedClasses = new ArrayList<>(classNames.length);
        for (String className : classNames) {
            annotatedClasses.add(asSourceClass(className));
        }
        return annotatedClasses;
    }

    SourceClass asSourceClass(@Nullable String className) throws IOException {
        if (className == null || className.startsWith("java.lang.annotation.")) {
            return this.objectSourceClass;
        }
        if (className.startsWith("java")) {
            // Never use ASM for core java types
            try {
                return new SourceClass(ClassUtils.forName(className, this.resourceLoader.getClassLoader()));
            } catch (ClassNotFoundException ex) {
                throw new NestedIOException("Failed to load class [" + className + "]", ex);
            }
        }
        return new SourceClass(this.metadataReaderFactory.getMetadataReader(className));
    }

    @SuppressWarnings("serial")
    private static class ImportStack extends ArrayDeque<ConfigurationClass> implements ImportRegistry {

        private final MultiValueMap<String, AnnotationMetadata> imports = new LinkedMultiValueMap<>();

        public void registerImport(AnnotationMetadata importingClass, String importedClass) {
            // 反的。。。嗯。。。
            this.imports.add(importedClass, importingClass);
        }

        @Override
        @Nullable
        public AnnotationMetadata getImportingClassFor(String importedClass) {
            // 最后一个元素
            return CollectionUtils.lastElement(this.imports.get(importedClass));
        }

        @Override
        public void removeImportingClass(String importingClass) {
            for (List<AnnotationMetadata> list : this.imports.values()) {
                for (Iterator<AnnotationMetadata> iterator = list.iterator(); iterator.hasNext(); ) {
                    if (iterator.next().getClassName().equals(importingClass)) {
                        iterator.remove();
                        break;
                    }
                }
            }
        }

        @Override
        public String toString() {
            StringJoiner joiner = new StringJoiner("->", "[", "]");
            for (ConfigurationClass configurationClass : this) {
                joiner.add(configurationClass.getSimpleName());
            }
            return joiner.toString();
        }

    }

    private class DeferredImportSelectorHandler {

        @Nullable
        private List<DeferredImportSelectorHolder> deferredImportSelectors = new ArrayList<>();

        public void handle(ConfigurationClass configClass, DeferredImportSelector importSelector) {
            DeferredImportSelectorHolder holder = new DeferredImportSelectorHolder(configClass, importSelector);
            if (this.deferredImportSelectors == null) {
                DeferredImportSelectorGroupingHandler handler = new DeferredImportSelectorGroupingHandler();
                handler.register(holder);
                handler.processGroupImports();
            } else {
                // 把DeferredImportSelector 加入到deferredImportSelectors集合类中，在parse方法里调用接口定义的方法
                this.deferredImportSelectors.add(holder);
            }
        }

        public void process() {
            List<DeferredImportSelectorHolder> deferredImports = this.deferredImportSelectors;
            this.deferredImportSelectors = null;
            try {
                if (deferredImports != null) {
                    DeferredImportSelectorGroupingHandler handler = new DeferredImportSelectorGroupingHandler();
                    deferredImports.sort(DEFERRED_IMPORT_COMPARATOR);
                    deferredImports.forEach(handler::register);
                    handler.processGroupImports();
                }
            } finally {
                this.deferredImportSelectors = new ArrayList<>();
            }
        }

    }

    private class DeferredImportSelectorGroupingHandler {

        private final Map<Object, DeferredImportSelectorGrouping> groupings = new LinkedHashMap<>();

        private final Map<AnnotationMetadata, ConfigurationClass> configurationClasses = new HashMap<>();

        public void register(DeferredImportSelectorHolder deferredImport) {
            Class<? extends Group> group = deferredImport.getImportSelector().getImportGroup();
            DeferredImportSelectorGrouping grouping = this.groupings.computeIfAbsent((group != null ? group : deferredImport), key -> new DeferredImportSelectorGrouping(createGroup(group)));
            grouping.add(deferredImport);
            this.configurationClasses.put(deferredImport.getConfigurationClass().getMetadata(), deferredImport.getConfigurationClass());
        }

        public void processGroupImports() {
            for (DeferredImportSelectorGrouping grouping : this.groupings.values()) {
                grouping.getImports().forEach(entry -> {
                    ConfigurationClass configurationClass = this.configurationClasses.get(entry.getMetadata());
                    try {
                        processImports(configurationClass, asSourceClass(configurationClass), asSourceClasses(entry.getImportClassName()), false);
                    } catch (BeanDefinitionStoreException ex) {
                        throw ex;
                    } catch (Throwable ex) {
                        throw new BeanDefinitionStoreException("Failed to process import candidates for configuration class [" + configurationClass.getMetadata().getClassName() + "]", ex);
                    }
                });
            }
        }

        private Group createGroup(@Nullable Class<? extends Group> type) {
            Class<? extends Group> effectiveType = (type != null ? type : DefaultDeferredImportSelectorGroup.class);
            Group group = BeanUtils.instantiateClass(effectiveType);
            ParserStrategyUtils.invokeAwareMethods(group, ConfigurationClassParser.this.environment, ConfigurationClassParser.this.resourceLoader, ConfigurationClassParser.this.registry);
            return group;
        }

    }

    private static class DeferredImportSelectorHolder {

        private final ConfigurationClass configurationClass;

        private final DeferredImportSelector importSelector;

        public DeferredImportSelectorHolder(ConfigurationClass configClass, DeferredImportSelector selector) {
            this.configurationClass = configClass;
            this.importSelector = selector;
        }

        public ConfigurationClass getConfigurationClass() {
            return this.configurationClass;
        }

        public DeferredImportSelector getImportSelector() {
            return this.importSelector;
        }

    }

    private static class DeferredImportSelectorGrouping {

        private final DeferredImportSelector.Group group;

        private final List<DeferredImportSelectorHolder> deferredImports = new ArrayList<>();

        DeferredImportSelectorGrouping(Group group) {
            this.group = group;
        }

        public void add(DeferredImportSelectorHolder deferredImport) {
            this.deferredImports.add(deferredImport);
        }

        public Iterable<Group.Entry> getImports() {
            for (DeferredImportSelectorHolder deferredImport : this.deferredImports) {
                this.group.process(deferredImport.getConfigurationClass().getMetadata(), deferredImport.getImportSelector());
            }
            return this.group.selectImports();
        }

    }

    private static class DefaultDeferredImportSelectorGroup implements Group {

        private final List<Entry> imports = new ArrayList<>();

        @Override
        public void process(AnnotationMetadata metadata, DeferredImportSelector selector) {
            for (String importClassName : selector.selectImports(metadata)) {
                this.imports.add(new Entry(metadata, importClassName));
            }
        }

        @Override
        public Iterable<Entry> selectImports() {
            return this.imports;
        }

    }

    private class SourceClass implements Ordered {

        private final Object source;  // Class or MetadataReader

        private final AnnotationMetadata metadata;

        public SourceClass(Object source) {
            this.source = source;
            if (source instanceof Class) {
                this.metadata = AnnotationMetadata.introspect((Class<?>) source);
            } else {
                this.metadata = ((MetadataReader) source).getAnnotationMetadata();
            }
        }

        public final AnnotationMetadata getMetadata() {
            return this.metadata;
        }

        @Override
        public int getOrder() {
            Integer order = ConfigurationClassUtils.getOrder(this.metadata);
            return (order != null ? order : Ordered.LOWEST_PRECEDENCE);
        }

        public Class<?> loadClass() throws ClassNotFoundException {
            if (this.source instanceof Class) {
                return (Class<?>) this.source;
            }
            String className = ((MetadataReader) this.source).getClassMetadata().getClassName();
            return ClassUtils.forName(className, resourceLoader.getClassLoader());
        }

        public boolean isAssignable(Class<?> clazz) throws IOException {
            if (this.source instanceof Class) {
                return clazz.isAssignableFrom((Class<?>) this.source);
            }
            return new AssignableTypeFilter(clazz).match((MetadataReader) this.source, metadataReaderFactory);
        }

        public ConfigurationClass asConfigClass(ConfigurationClass importedBy) {
            if (this.source instanceof Class) {
                return new ConfigurationClass((Class<?>) this.source, importedBy);
            }
            return new ConfigurationClass((MetadataReader) this.source, importedBy);
        }

        public Collection<SourceClass> getMemberClasses() throws IOException {
            Object sourceToProcess = this.source;
            if (sourceToProcess instanceof Class) {
                Class<?> sourceClass = (Class<?>) sourceToProcess;
                try {
                    Class<?>[] declaredClasses = sourceClass.getDeclaredClasses();
                    List<SourceClass> members = new ArrayList<>(declaredClasses.length);
                    for (Class<?> declaredClass : declaredClasses) {
                        members.add(asSourceClass(declaredClass));
                    }
                    return members;
                } catch (NoClassDefFoundError err) {
                    // getDeclaredClasses() failed because of non-resolvable dependencies
                    // -> fall back to ASM below
                    sourceToProcess = metadataReaderFactory.getMetadataReader(sourceClass.getName());
                }
            }
            // ASM-based resolution - safe for non-resolvable classes as well
            MetadataReader sourceReader = (MetadataReader) sourceToProcess;
            String[] memberClassNames = sourceReader.getClassMetadata().getMemberClassNames();
            List<SourceClass> members = new ArrayList<>(memberClassNames.length);
            for (String memberClassName : memberClassNames) {
                try {
                    members.add(asSourceClass(memberClassName));
                } catch (IOException ex) {
                    // Let's skip it if it's not resolvable - we're just looking for candidates
                    if (logger.isDebugEnabled()) {
                        logger.debug("Failed to resolve member class [" + memberClassName + "] - not considering it as a configuration class candidate");
                    }
                }
            }
            return members;
        }

        public SourceClass getSuperClass() throws IOException {
            if (this.source instanceof Class) {
                return asSourceClass(((Class<?>) this.source).getSuperclass());
            }
            return asSourceClass(((MetadataReader) this.source).getClassMetadata().getSuperClassName());
        }

        public Set<SourceClass> getInterfaces() throws IOException {
            Set<SourceClass> result = new LinkedHashSet<>();
            if (this.source instanceof Class) {
                Class<?> sourceClass = (Class<?>) this.source;
                for (Class<?> ifcClass : sourceClass.getInterfaces()) {
                    result.add(asSourceClass(ifcClass));
                }
            } else {
                for (String className : this.metadata.getInterfaceNames()) {
                    result.add(asSourceClass(className));
                }
            }
            return result;
        }

        public Set<SourceClass> getAnnotations() {
            Set<SourceClass> result = new LinkedHashSet<>();
            if (this.source instanceof Class) {
                Class<?> sourceClass = (Class<?>) this.source;
                for (Annotation ann : sourceClass.getDeclaredAnnotations()) {
                    Class<?> annType = ann.annotationType();
                    if (!annType.getName().startsWith("java")) {
                        try {
                            result.add(asSourceClass(annType));
                        } catch (Throwable ex) {
                            // An annotation not present on the classpath is being ignored
                            // by the JVM's class loading -> ignore here as well.
                        }
                    }
                }
            } else {
                for (String className : this.metadata.getAnnotationTypes()) {
                    if (!className.startsWith("java")) {
                        try {
                            result.add(getRelated(className));
                        } catch (Throwable ex) {
                            // An annotation not present on the classpath is being ignored
                            // by the JVM's class loading -> ignore here as well.
                        }
                    }
                }
            }
            return result;
        }

        public Collection<SourceClass> getAnnotationAttributes(String annType, String attribute) throws IOException {
            Map<String, Object> annotationAttributes = this.metadata.getAnnotationAttributes(annType, true);
            if (annotationAttributes == null || !annotationAttributes.containsKey(attribute)) {
                return Collections.emptySet();
            }
            String[] classNames = (String[]) annotationAttributes.get(attribute);
            Set<SourceClass> result = new LinkedHashSet<>();
            for (String className : classNames) {
                result.add(getRelated(className));
            }
            return result;
        }

        private SourceClass getRelated(String className) throws IOException {
            if (this.source instanceof Class) {
                try {
                    Class<?> clazz = ClassUtils.forName(className, ((Class<?>) this.source).getClassLoader());
                    return asSourceClass(clazz);
                } catch (ClassNotFoundException ex) {
                    // Ignore -> fall back to ASM next, except for core java types.
                    if (className.startsWith("java")) {
                        throw new NestedIOException("Failed to load class [" + className + "]", ex);
                    }
                    return new SourceClass(metadataReaderFactory.getMetadataReader(className));
                }
            }
            return asSourceClass(className);
        }

        @Override
        public boolean equals(Object other) {
            return (this == other || (other instanceof SourceClass && this.metadata.getClassName().equals(((SourceClass) other).metadata.getClassName())));
        }

        @Override
        public int hashCode() {
            return this.metadata.getClassName().hashCode();
        }

        @Override
        public String toString() {
            return this.metadata.getClassName();
        }

    }

    private static class CircularImportProblem extends Problem {

        public CircularImportProblem(ConfigurationClass attemptedImport, Deque<ConfigurationClass> importStack) {
            super(String.format("A circular @Import has been detected: " + "Illegal attempt by @Configuration class '%s' to import class '%s' as '%s' is " + "already present in the current import stack %s", importStack.element().getSimpleName(), attemptedImport.getSimpleName(), attemptedImport.getSimpleName(), importStack), new Location(importStack.element().getResource(), attemptedImport.getMetadata()));
        }

    }

}
