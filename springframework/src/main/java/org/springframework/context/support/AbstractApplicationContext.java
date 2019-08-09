package org.springframework.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.CachedIntrospectionResults;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.support.ResourceEditorRegistrar;
import org.springframework.context.*;
import org.springframework.context.event.*;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.context.weaving.LoadTimeWeaverAware;
import org.springframework.context.weaving.LoadTimeWeaverAwareProcessor;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractApplicationContext extends DefaultResourceLoader implements ConfigurableApplicationContext {

    public static final String MESSAGE_SOURCE_BEAN_NAME = "messageSource";

    public static final String LIFECYCLE_PROCESSOR_BEAN_NAME = "lifecycleProcessor";

    public static final String APPLICATION_EVENT_MULTICASTER_BEAN_NAME = "applicationEventMulticaster";

    static {
        // Eagerly load the ContextClosedEvent class to avoid weird classloader issues
        // on application shutdown in WebLogic 8.1. (Reported by Dustin Woods.)
        ContextClosedEvent.class.getName();
    }

    protected final Log logger = LogFactory.getLog(getClass());

    private String id = ObjectUtils.identityToString(this);

    private String displayName = ObjectUtils.identityToString(this);

    @Nullable
    private ApplicationContext parent;

    @Nullable
    private ConfigurableEnvironment environment;

    private final List<BeanFactoryPostProcessor> beanFactoryPostProcessors = new ArrayList<>();

    private long startupDate;

    private final AtomicBoolean active = new AtomicBoolean();

    private final AtomicBoolean closed = new AtomicBoolean();

    private final Object startupShutdownMonitor = new Object();

    @Nullable
    private Thread shutdownHook;

    private ResourcePatternResolver resourcePatternResolver;

    @Nullable
    private LifecycleProcessor lifecycleProcessor;

    @Nullable
    private MessageSource messageSource;

    @Nullable
    private ApplicationEventMulticaster applicationEventMulticaster;

    private final Set<ApplicationListener<?>> applicationListeners = new LinkedHashSet<>();

    @Nullable
    private Set<ApplicationListener<?>> earlyApplicationListeners;

    @Nullable
    private Set<ApplicationEvent> earlyApplicationEvents;

    public AbstractApplicationContext() {
        this.resourcePatternResolver = getResourcePatternResolver();
    }

    public AbstractApplicationContext(@Nullable ApplicationContext parent) {
        this();
        setParent(parent);
    }
    //---------------------------------------------------------------------
    // Implementation of ApplicationContext interface
    //---------------------------------------------------------------------

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getApplicationName() {
        return "";
    }

    public void setDisplayName(String displayName) {
        Assert.hasLength(displayName, "Display name must not be empty");
        this.displayName = displayName;
    }

    @Override
    public String getDisplayName() {
        return this.displayName;
    }

    @Override
    @Nullable
    public ApplicationContext getParent() {
        return this.parent;
    }

    @Override
    public void setEnvironment(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public ConfigurableEnvironment getEnvironment() {
        if (this.environment == null) {
            this.environment = createEnvironment();
        }
        return this.environment;
    }

    protected ConfigurableEnvironment createEnvironment() {
        return new StandardEnvironment();
    }

    @Override
    public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
        return getBeanFactory();
    }

    @Override
    public long getStartupDate() {
        return this.startupDate;
    }

    @Override
    public void publishEvent(ApplicationEvent event) {
        publishEvent(event, null);
    }

    @Override
    public void publishEvent(Object event) {
        publishEvent(event, null);
    }

    protected void publishEvent(Object event, @Nullable ResolvableType eventType) {
        Assert.notNull(event, "Event must not be null");
        // Decorate event as an ApplicationEvent if necessary
        ApplicationEvent applicationEvent;
        if (event instanceof ApplicationEvent) {
            applicationEvent = (ApplicationEvent) event;
        } else {
            applicationEvent = new PayloadApplicationEvent<>(this, event);
            if (eventType == null) {
                eventType = ((PayloadApplicationEvent<?>) applicationEvent).getResolvableType();
            }
        }
        // Multicast right now if possible - or lazily once the multicaster is initialized
        if (this.earlyApplicationEvents != null) {
            this.earlyApplicationEvents.add(applicationEvent);
        } else {
            getApplicationEventMulticaster().multicastEvent(applicationEvent, eventType);
        }
        // Publish event via parent context as well...
        if (this.parent != null) {
            if (this.parent instanceof AbstractApplicationContext) {
                ((AbstractApplicationContext) this.parent).publishEvent(event, eventType);
            } else {
                this.parent.publishEvent(event);
            }
        }
    }

    ApplicationEventMulticaster getApplicationEventMulticaster() throws IllegalStateException {
        if (this.applicationEventMulticaster == null) {
            throw new IllegalStateException("ApplicationEventMulticaster not initialized - " + "call 'refresh' before multicasting events via the context: " + this);
        }
        return this.applicationEventMulticaster;
    }

    LifecycleProcessor getLifecycleProcessor() throws IllegalStateException {
        if (this.lifecycleProcessor == null) {
            throw new IllegalStateException("LifecycleProcessor not initialized - " + "call 'refresh' before invoking lifecycle methods via the context: " + this);
        }
        return this.lifecycleProcessor;
    }

    protected ResourcePatternResolver getResourcePatternResolver() {
        return new PathMatchingResourcePatternResolver(this);
    }
    //---------------------------------------------------------------------
    // Implementation of ConfigurableApplicationContext interface
    //---------------------------------------------------------------------

    @Override
    public void setParent(@Nullable ApplicationContext parent) {
        this.parent = parent;
        if (parent != null) {
            Environment parentEnvironment = parent.getEnvironment();
            if (parentEnvironment instanceof ConfigurableEnvironment) {
                getEnvironment().merge((ConfigurableEnvironment) parentEnvironment);
            }
        }
    }

    @Override
    public void addBeanFactoryPostProcessor(BeanFactoryPostProcessor postProcessor) {
        Assert.notNull(postProcessor, "BeanFactoryPostProcessor must not be null");
        this.beanFactoryPostProcessors.add(postProcessor);
    }

    public List<BeanFactoryPostProcessor> getBeanFactoryPostProcessors() {
        return this.beanFactoryPostProcessors;
    }

    @Override
    public void addApplicationListener(ApplicationListener<?> listener) {
        Assert.notNull(listener, "ApplicationListener must not be null");
        if (this.applicationEventMulticaster != null) {
            this.applicationEventMulticaster.addApplicationListener(listener);
        }
        this.applicationListeners.add(listener);
    }

    public Collection<ApplicationListener<?>> getApplicationListeners() {
        return this.applicationListeners;
    }

    @Override
    public void refresh() throws BeansException, IllegalStateException {
        // 加锁
        synchronized (this.startupShutdownMonitor) {
            // 准备刷新，生成Environment，略
            prepareRefresh();
            /*
             * XML：
             *      销毁BeanFactory，再重新初始化
             *      解析XML配置成BeanDefinition，注册到BeanFactory，映射beanName到beanDefinition，映射alias到beanName，Bean未初始化
             * 注解：
             *      BeanFactory在ApplicationContext实例化时一起实例化，不销毁，只是加个更新标记
             */
            ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();
            /*
             * 准备BeanFactory：
             *       设置BeanFactory的类加载器为加载当前ApplicationContext类的类加载器
             *       实例化并添加几个BeanPostProcessor：
             *           添加ApplicationContextAwareProcessor到beanPostProcessors列表,负责Aware接口的Bean初始化回调
             *       如果BeanFactory包含LOAD_TIME_WEAVER的Bean：
             *           实例化LoadTimeWeaverAwareProcessor并添加，同时给BeanFactory设置临时类加载器，避免类型匹配时提前加载类
             *       如果BeanFactory未包含，添加几个单例Bean（Environment等加入BeanFactory）
             */
            prepareBeanFactory(beanFactory);
            try {
                // 子类重写
                postProcessBeanFactory(beanFactory);
                /*
                 * 对已实例化添加的BeanDefinitionRegistryPostProcessor
                 *      遍历已实例化添加的BeanFactoryPostProcessor，如果是BeanDefinitionRegistryPostProcessor实例，调用postProcessBeanDefinitionRegistry
                 * 对未实例化的BeanDefinitionRegistryPostProcessor
                 *      从beanDefinitionNames和manualSingletonNames获取已注册的BeanDefinitionRegistryPostProcessor类型的beanName
                 *      按照是否实现了PriorityOrdered、Ordered接口的顺序，分三批调用BeanFactory#getBean实例化，调用postProcessBeanDefinitionRegistry
                 * 遍历所有已实例化的BeanDefinitionRegistryPostProcessor类型的BeanFactoryPostProcessor，调用postProcessBeanFactory
                 * 遍历所有已实例化的非BeanDefinitionRegistryPostProcessor类型的BeanFactoryPostProcessor，调用postProcessBeanFactory
                 * 对未实例化的BeanFactoryPostProcessor
                 *      从beanDefinitionNames和manualSingletonNames获取已注册的BeanFactoryPostProcessor类型的beanName，遍历，跳过上面已处理过的BeanFactoryPostProcessor
                 *      按照是否实现了PriorityOrdered、Ordered接口的顺序，分三批调用BeanFactory#getBean实例化，调用postProcessBeanDefinitionRegistry
                 * 如果BeanFactory包含LOAD_TIME_WEAVER的Bean并且没设置临时类加载器：
                 *      实例化LoadTimeWeaverAwareProcessor并添加，同时给BeanFactory设置临时类加载器，避免类型匹配时提前加载类
                 */
                invokeBeanFactoryPostProcessors(beanFactory);
                /*
                 * 注册BeanPostProcessor的实现类，postProcessBeforeInitialization、postProcessAfterInitialization分别会在Bean初始化前后执行
                 *      从beanDefinitionNames和manualSingletonNames获取已注册的BeanPostProcessor的beanName
                 *      按照是否实现了PriorityOrdered、Ordered接口的顺序，分三批调用BeanFactory#getBean实例化，并添加实例到BeanFactory的beanPostProcessors
                 *      对于实现了MergedBeanDefinitionPostProcessor接口的，再覆盖添加实例到BeanFactory的beanPostProcessors
                 *      覆盖添加新的ApplicationListenerDetector到BeanFactory的beanPostProcessors
                 */
                registerBeanPostProcessors(beanFactory);
                // 初始化MessageSource，国际化，略
                initMessageSource();
                // 初始化当前ApplicationContext的事件广播器，略
                initApplicationEventMulticaster();
                // 模板方法，子类重写
                onRefresh();
                /*
                 * 添加已有的ApplicationListener到当前的事件广播器
                 * 从beanDefinitionNames和manualSingletonNames获取已注册的ApplicationListener的beanName，添加名称到当前的事件广播器
                 * 通过事件广播器广播earlyApplicationEvents
                 */
                registerListeners();
                /*
                 * 如果singletonObjects或beanDefinitionMap包含名为conversionService的Bean，BeanFactory#getBean初始化，并为当前BeanFactory设置ConversionService
                 * 如果最后还没有内置的EmbeddedValueResolver，添加一个StringValueResolver进去，注入依赖时用来解析@Value(value = "${xxx.xxx}")
                 * 从beanDefinitionNames和manualSingletonNames获取已注册的LoadTimeWeaverAware的beanName，调用BeanFactory#getBean初始化
                 * 置空临时类加载器
                 * 初始化剩下所有非懒加载的单例，如果Bean实现了SmartInitializingSingleton接口，触发afterSingletonsInstantiated回调
                 */
                finishBeanFactoryInitialization(beanFactory);
                /*
                 * 初始化LifecycleProcessor，添加到BeanFactory，调用LifecycleProcessor的onRefresh
                 * 通过通过事件广播器广播ContextRefreshedEvent
                 * 子类可重写
                 */
                finishRefresh();
            } catch (BeansException ex) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Exception encountered during context initialization - " + "cancelling refresh attempt: " + ex);
                }
                // 销毁已初始化的单例，避免占用资源
                destroyBeans();
                // Reset 'active' flag.
                cancelRefresh(ex);
                // 传播异常给调用者
                throw ex;
            } finally {
                // Reset common introspection caches in Spring's core, since we
                // might not ever need metadata for singleton beans anymore...
                resetCommonCaches();
            }
        }
    }

    protected void prepareRefresh() {
        // Switch to active.
        this.startupDate = System.currentTimeMillis();
        this.closed.set(false);
        this.active.set(true);
        if (logger.isDebugEnabled()) {
            if (logger.isTraceEnabled()) {
                logger.trace("Refreshing " + this);
            } else {
                logger.debug("Refreshing " + getDisplayName());
            }
        }
        // Initialize any placeholder property sources in the context environment.
        initPropertySources();
        // Validate that all properties marked as required are resolvable:
        // see ConfigurablePropertyResolver#setRequiredProperties
        getEnvironment().validateRequiredProperties();
        // Store pre-refresh ApplicationListeners...
        if (this.earlyApplicationListeners == null) {
            this.earlyApplicationListeners = new LinkedHashSet<>(this.applicationListeners);
        } else {
            // Reset local application listeners to pre-refresh state.
            this.applicationListeners.clear();
            this.applicationListeners.addAll(this.earlyApplicationListeners);
        }
        // Allow for the collection of early ApplicationEvents,
        // to be published once the multicaster is available...
        this.earlyApplicationEvents = new LinkedHashSet<>();
    }

    protected void initPropertySources() {
        // For subclasses: do nothing by default.
    }

    protected ConfigurableListableBeanFactory obtainFreshBeanFactory() {
        // XML：关闭旧的BeanFactory(如果有)，创建新的BeanFactory，加载BeanDefinition
        // 注解：加个更新标记
        refreshBeanFactory();
        // 返回刚创建的BeanFactory
        return getBeanFactory();
    }

    /**
     * 准备BeanFactory
     */
    protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        // 设置BeanFactory的类加载器为加载当前ApplicationContext类的类加载器
        beanFactory.setBeanClassLoader(getClassLoader());
        // 设置BeanExpressionResolver
        beanFactory.setBeanExpressionResolver(new StandardBeanExpressionResolver(beanFactory.getBeanClassLoader()));
        // 设置PropertyEditorRegistrar
        beanFactory.addPropertyEditorRegistrar(new ResourceEditorRegistrar(this, getEnvironment()));
        // 添加ApplicationContextAwareProcessor到beanPostProcessors列表
        // ApplicationContextAwareProcessor负责Aware接口的Bean初始化回调，ApplicationContextAware，EnvironmentAware、ResourceLoaderAware等
        beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));
        // 添加到ignoredDependencyInterfaces，如果某个Bean依赖于以下几个接口的实现类，在自动装配的时候忽略它们
        beanFactory.ignoreDependencyInterface(EnvironmentAware.class);
        beanFactory.ignoreDependencyInterface(EmbeddedValueResolverAware.class);
        beanFactory.ignoreDependencyInterface(ResourceLoaderAware.class);
        beanFactory.ignoreDependencyInterface(ApplicationEventPublisherAware.class);
        beanFactory.ignoreDependencyInterface(MessageSourceAware.class);
        beanFactory.ignoreDependencyInterface(ApplicationContextAware.class);
        /*
         * 添加到resolvableDependencies映射
         * 为几个特殊的Bean赋值，如果有Bean依赖了以下几个，注入这边相应的值
         * ApplicationContext继承了BeanFactory、ResourceLoader、ApplicationEventPublisher、MessageSource，可以赋值为this
         * MessageSource被注册为普通的Bean
         */
        beanFactory.registerResolvableDependency(BeanFactory.class, beanFactory);
        beanFactory.registerResolvableDependency(ResourceLoader.class, this);
        beanFactory.registerResolvableDependency(ApplicationEventPublisher.class, this);
        beanFactory.registerResolvableDependency(ApplicationContext.class, this);
        // 实例化ApplicationListenerDetector添加到beanPostProcessors列表
        // ApplicationListenerDetector负责注册事件监听，Bean实例化后，如果是ApplicationListener的子类，将其添加到listener列表
        beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(this));
        // LoadTimeWeaver，AspectJ，运行期织入，略
        if (beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
            beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
            // Set a temporary ClassLoader for type matching.
            beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
        }
        // 如果没有environment的Bean，添加一个默认
        if (!beanFactory.containsLocalBean(ENVIRONMENT_BEAN_NAME)) {
            beanFactory.registerSingleton(ENVIRONMENT_BEAN_NAME, getEnvironment());
        }
        // 如果没有systemProperties的Bean，添加一个默认
        if (!beanFactory.containsLocalBean(SYSTEM_PROPERTIES_BEAN_NAME)) {
            beanFactory.registerSingleton(SYSTEM_PROPERTIES_BEAN_NAME, getEnvironment().getSystemProperties());
        }
        // 如果没有systemEnvironment的Bean，添加一个默认
        if (!beanFactory.containsLocalBean(SYSTEM_ENVIRONMENT_BEAN_NAME)) {
            beanFactory.registerSingleton(SYSTEM_ENVIRONMENT_BEAN_NAME, getEnvironment().getSystemEnvironment());
        }
    }

    protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    }

    protected void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory) {
        //
        PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(beanFactory, getBeanFactoryPostProcessors());
        // Detect a LoadTimeWeaver and prepare for weaving, if found in the meantime
        // (e.g. through an @Bean method registered by ConfigurationClassPostProcessor)
        if (beanFactory.getTempClassLoader() == null && beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
            beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
            beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
        }
    }

    protected void registerBeanPostProcessors(ConfigurableListableBeanFactory beanFactory) {
        PostProcessorRegistrationDelegate.registerBeanPostProcessors(beanFactory, this);
    }

    protected void initMessageSource() {
        ConfigurableListableBeanFactory beanFactory = getBeanFactory();
        if (beanFactory.containsLocalBean(MESSAGE_SOURCE_BEAN_NAME)) {
            this.messageSource = beanFactory.getBean(MESSAGE_SOURCE_BEAN_NAME, MessageSource.class);
            // Make MessageSource aware of parent MessageSource.
            if (this.parent != null && this.messageSource instanceof HierarchicalMessageSource) {
                HierarchicalMessageSource hms = (HierarchicalMessageSource) this.messageSource;
                if (hms.getParentMessageSource() == null) {
                    // Only set parent context as parent MessageSource if no parent MessageSource
                    // registered already.
                    hms.setParentMessageSource(getInternalParentMessageSource());
                }
            }
            if (logger.isTraceEnabled()) {
                logger.trace("Using MessageSource [" + this.messageSource + "]");
            }
        } else {
            // Use empty MessageSource to be able to accept getMessage calls.
            DelegatingMessageSource dms = new DelegatingMessageSource();
            dms.setParentMessageSource(getInternalParentMessageSource());
            this.messageSource = dms;
            beanFactory.registerSingleton(MESSAGE_SOURCE_BEAN_NAME, this.messageSource);
            if (logger.isTraceEnabled()) {
                logger.trace("No '" + MESSAGE_SOURCE_BEAN_NAME + "' bean, using [" + this.messageSource + "]");
            }
        }
    }

    protected void initApplicationEventMulticaster() {
        ConfigurableListableBeanFactory beanFactory = getBeanFactory();
        // 如果已经有了
        if (beanFactory.containsLocalBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME)) {
            this.applicationEventMulticaster = beanFactory.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, ApplicationEventMulticaster.class);
            if (logger.isTraceEnabled()) {
                logger.trace("Using ApplicationEventMulticaster [" + this.applicationEventMulticaster + "]");
            }
        }
        // 不存在
        else {
            // 新建一个
            this.applicationEventMulticaster = new SimpleApplicationEventMulticaster(beanFactory);
            // 添加
            beanFactory.registerSingleton(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, this.applicationEventMulticaster);
            if (logger.isTraceEnabled()) {
                logger.trace("No '" + APPLICATION_EVENT_MULTICASTER_BEAN_NAME + "' bean, using " + "[" + this.applicationEventMulticaster.getClass().getSimpleName() + "]");
            }
        }
    }

    protected void initLifecycleProcessor() {
        ConfigurableListableBeanFactory beanFactory = getBeanFactory();
        if (beanFactory.containsLocalBean(LIFECYCLE_PROCESSOR_BEAN_NAME)) {
            this.lifecycleProcessor = beanFactory.getBean(LIFECYCLE_PROCESSOR_BEAN_NAME, LifecycleProcessor.class);
            if (logger.isTraceEnabled()) {
                logger.trace("Using LifecycleProcessor [" + this.lifecycleProcessor + "]");
            }
        } else {
            DefaultLifecycleProcessor defaultProcessor = new DefaultLifecycleProcessor();
            defaultProcessor.setBeanFactory(beanFactory);
            this.lifecycleProcessor = defaultProcessor;
            beanFactory.registerSingleton(LIFECYCLE_PROCESSOR_BEAN_NAME, this.lifecycleProcessor);
            if (logger.isTraceEnabled()) {
                logger.trace("No '" + LIFECYCLE_PROCESSOR_BEAN_NAME + "' bean, using " + "[" + this.lifecycleProcessor.getClass().getSimpleName() + "]");
            }
        }
    }

    protected void onRefresh() throws BeansException {
        // For subclasses: do nothing by default.
    }

    protected void registerListeners() {
        // 遍历现有的applicationListeners列表
        for (ApplicationListener<?> listener : getApplicationListeners()) {
            // 添加ApplicationListener到当前的广播器
            getApplicationEventMulticaster().addApplicationListener(listener);
        }
        // Do not initialize FactoryBeans here: We need to leave all regular beans
        // uninitialized to let post-processors apply to them!
        // 获取现有的applicationListeners名称列表
        String[] listenerBeanNames = getBeanNamesForType(ApplicationListener.class, true, false);
        // 遍历
        for (String listenerBeanName : listenerBeanNames) {
            // 添加ApplicationListener名称到当前的广播器
            getApplicationEventMulticaster().addApplicationListenerBean(listenerBeanName);
        }
        // 发布早期ApplicationEvent
        Set<ApplicationEvent> earlyEventsToProcess = this.earlyApplicationEvents;
        this.earlyApplicationEvents = null;
        if (earlyEventsToProcess != null) {
            for (ApplicationEvent earlyEvent : earlyEventsToProcess) {
                // 广播
                getApplicationEventMulticaster().multicastEvent(earlyEvent);
            }
        }
    }

    protected void finishBeanFactoryInitialization(ConfigurableListableBeanFactory beanFactory) {
        // 如果singletonObjects或beanDefinitionMap包含名为conversionService的Bean，BeanFactory#getBean初始化，并为当前BeanFactory设置ConversionService
        if (beanFactory.containsBean(CONVERSION_SERVICE_BEAN_NAME) && beanFactory.isTypeMatch(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class)) {
            beanFactory.setConversionService(beanFactory.getBean(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class));
        }
        // Register a default embedded value resolver if no bean post-processor
        // (such as a PropertyPlaceholderConfigurer bean) registered any before:
        // at this point, primarily for resolution in annotation attribute values.
        // 如果最后还没有内置的EmbeddedValueResolver，添加一个StringValueResolver进去，注入依赖时用来解析@Value(value = "${xxx.xxx}")
        if (!beanFactory.hasEmbeddedValueResolver()) {
            beanFactory.addEmbeddedValueResolver(strVal -> getEnvironment().resolvePlaceholders(strVal));
        }
        // 从beanDefinitionNames和manualSingletonNames获取已注册的LoadTimeWeaverAware的beanName，调用BeanFactory#getBean初始化
        // Initialize LoadTimeWeaverAware beans early to allow for registering their transformers early.
        String[] weaverAwareNames = beanFactory.getBeanNamesForType(LoadTimeWeaverAware.class, false, false);
        for (String weaverAwareName : weaverAwareNames) {
            getBean(weaverAwareName);
        }
        // 类型匹配时不再用临时类加载器
        beanFactory.setTempClassLoader(null);
        // 不希望BeanDefinition出现变化
        beanFactory.freezeConfiguration();
        // 初始化剩下所有非懒加载的单例，如果Bean实现了SmartInitializingSingleton接口，触发afterSingletonsInstantiated回调
        beanFactory.preInstantiateSingletons();
    }

    /**
     * 完成刷新
     */
    protected void finishRefresh() {
        // Clear context-level resource caches (such as ASM metadata from scanning).
        clearResourceCaches();
        // 初始化LifecycleProcessor，添加到BeanFactory
        initLifecycleProcessor();
        // 先传播刷新给LifecycleProcessor
        getLifecycleProcessor().onRefresh();
        // 发布刷新完成事件
        publishEvent(new ContextRefreshedEvent(this));
        // Participate in LiveBeansView MBean, if active.
        LiveBeansView.registerApplicationContext(this);
    }

    protected void cancelRefresh(BeansException ex) {
        this.active.set(false);
    }

    protected void resetCommonCaches() {
        ReflectionUtils.clearCache();
        AnnotationUtils.clearCache();
        ResolvableType.clearCache();
        CachedIntrospectionResults.clearClassLoader(getClassLoader());
    }

    @Override
    public void registerShutdownHook() {
        if (this.shutdownHook == null) {
            // No shutdown hook registered yet.
            this.shutdownHook = new Thread() {
                @Override
                public void run() {
                    synchronized (startupShutdownMonitor) {
                        doClose();
                    }
                }
            };
            Runtime.getRuntime().addShutdownHook(this.shutdownHook);
        }
    }

    @Deprecated
    public void destroy() {
        close();
    }

    @Override
    public void close() {
        synchronized (this.startupShutdownMonitor) {
            doClose();
            // If we registered a JVM shutdown hook, we don't need it anymore now:
            // We've already explicitly closed the context.
            if (this.shutdownHook != null) {
                try {
                    Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
                } catch (IllegalStateException ex) {
                    // ignore - VM is already shutting down
                }
            }
        }
    }

    protected void doClose() {
        // Check whether an actual close attempt is necessary...
        if (this.active.get() && this.closed.compareAndSet(false, true)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Closing " + this);
            }
            LiveBeansView.unregisterApplicationContext(this);
            try {
                // Publish shutdown event.
                publishEvent(new ContextClosedEvent(this));
            } catch (Throwable ex) {
                logger.warn("Exception thrown from ApplicationListener handling ContextClosedEvent", ex);
            }
            // Stop all Lifecycle beans, to avoid delays during individual destruction.
            if (this.lifecycleProcessor != null) {
                try {
                    this.lifecycleProcessor.onClose();
                } catch (Throwable ex) {
                    logger.warn("Exception thrown from LifecycleProcessor on context close", ex);
                }
            }
            // Destroy all cached singletons in the context's BeanFactory.
            destroyBeans();
            // Close the state of this context itself.
            closeBeanFactory();
            // Let subclasses do some final clean-up if they wish...
            onClose();
            // Reset local application listeners to pre-refresh state.
            if (this.earlyApplicationListeners != null) {
                this.applicationListeners.clear();
                this.applicationListeners.addAll(this.earlyApplicationListeners);
            }
            // Switch to inactive.
            this.active.set(false);
        }
    }

    protected void destroyBeans() {
        getBeanFactory().destroySingletons();
    }

    protected void onClose() {
        // For subclasses: do nothing by default.
    }

    @Override
    public boolean isActive() {
        return this.active.get();
    }

    protected void assertBeanFactoryActive() {
        if (!this.active.get()) {
            if (this.closed.get()) {
                throw new IllegalStateException(getDisplayName() + " has been closed already");
            } else {
                throw new IllegalStateException(getDisplayName() + " has not been refreshed yet");
            }
        }
    }
    //---------------------------------------------------------------------
    // Implementation of BeanFactory interface
    //---------------------------------------------------------------------

    @Override
    public Object getBean(String name) throws BeansException {
        assertBeanFactoryActive();
        return getBeanFactory().getBean(name);
    }

    @Override
    public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
        assertBeanFactoryActive();
        return getBeanFactory().getBean(name, requiredType);
    }

    @Override
    public Object getBean(String name, Object... args) throws BeansException {
        assertBeanFactoryActive();
        return getBeanFactory().getBean(name, args);
    }

    @Override
    public <T> T getBean(Class<T> requiredType) throws BeansException {
        assertBeanFactoryActive();
        return getBeanFactory().getBean(requiredType);
    }

    @Override
    public <T> T getBean(Class<T> requiredType, Object... args) throws BeansException {
        assertBeanFactoryActive();
        return getBeanFactory().getBean(requiredType, args);
    }

    @Override
    public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType) {
        assertBeanFactoryActive();
        return getBeanFactory().getBeanProvider(requiredType);
    }

    @Override
    public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType) {
        assertBeanFactoryActive();
        return getBeanFactory().getBeanProvider(requiredType);
    }

    @Override
    public boolean containsBean(String name) {
        return getBeanFactory().containsBean(name);
    }

    @Override
    public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
        assertBeanFactoryActive();
        return getBeanFactory().isSingleton(name);
    }

    @Override
    public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
        assertBeanFactoryActive();
        return getBeanFactory().isPrototype(name);
    }

    @Override
    public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
        assertBeanFactoryActive();
        return getBeanFactory().isTypeMatch(name, typeToMatch);
    }

    @Override
    public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
        assertBeanFactoryActive();
        return getBeanFactory().isTypeMatch(name, typeToMatch);
    }

    @Override
    @Nullable
    public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
        assertBeanFactoryActive();
        return getBeanFactory().getType(name);
    }

    @Override
    public String[] getAliases(String name) {
        return getBeanFactory().getAliases(name);
    }
    //---------------------------------------------------------------------
    // Implementation of ListableBeanFactory interface
    //---------------------------------------------------------------------

    @Override
    public boolean containsBeanDefinition(String beanName) {
        return getBeanFactory().containsBeanDefinition(beanName);
    }

    @Override
    public int getBeanDefinitionCount() {
        return getBeanFactory().getBeanDefinitionCount();
    }

    @Override
    public String[] getBeanDefinitionNames() {
        return getBeanFactory().getBeanDefinitionNames();
    }

    @Override
    public String[] getBeanNamesForType(ResolvableType type) {
        assertBeanFactoryActive();
        return getBeanFactory().getBeanNamesForType(type);
    }

    @Override
    public String[] getBeanNamesForType(@Nullable Class<?> type) {
        assertBeanFactoryActive();
        return getBeanFactory().getBeanNamesForType(type);
    }

    @Override
    public String[] getBeanNamesForType(@Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
        assertBeanFactoryActive();
        return getBeanFactory().getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
    }

    @Override
    public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type) throws BeansException {
        assertBeanFactoryActive();
        return getBeanFactory().getBeansOfType(type);
    }

    @Override
    public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type, boolean includeNonSingletons, boolean allowEagerInit) throws BeansException {
        assertBeanFactoryActive();
        return getBeanFactory().getBeansOfType(type, includeNonSingletons, allowEagerInit);
    }

    @Override
    public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
        assertBeanFactoryActive();
        return getBeanFactory().getBeanNamesForAnnotation(annotationType);
    }

    @Override
    public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) throws BeansException {
        assertBeanFactoryActive();
        return getBeanFactory().getBeansWithAnnotation(annotationType);
    }

    @Override
    @Nullable
    public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType) throws NoSuchBeanDefinitionException {
        assertBeanFactoryActive();
        return getBeanFactory().findAnnotationOnBean(beanName, annotationType);
    }
    //---------------------------------------------------------------------
    // Implementation of HierarchicalBeanFactory interface
    //---------------------------------------------------------------------

    @Override
    @Nullable
    public BeanFactory getParentBeanFactory() {
        return getParent();
    }

    @Override
    public boolean containsLocalBean(String name) {
        return getBeanFactory().containsLocalBean(name);
    }

    @Nullable
    protected BeanFactory getInternalParentBeanFactory() {
        return (getParent() instanceof ConfigurableApplicationContext ? ((ConfigurableApplicationContext) getParent()).getBeanFactory() : getParent());
    }
    //---------------------------------------------------------------------
    // Implementation of MessageSource interface
    //---------------------------------------------------------------------

    @Override
    public String getMessage(String code, @Nullable Object[] args, @Nullable String defaultMessage, Locale locale) {
        return getMessageSource().getMessage(code, args, defaultMessage, locale);
    }

    @Override
    public String getMessage(String code, @Nullable Object[] args, Locale locale) throws NoSuchMessageException {
        return getMessageSource().getMessage(code, args, locale);
    }

    @Override
    public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
        return getMessageSource().getMessage(resolvable, locale);
    }

    private MessageSource getMessageSource() throws IllegalStateException {
        if (this.messageSource == null) {
            throw new IllegalStateException("MessageSource not initialized - " + "call 'refresh' before accessing messages via the context: " + this);
        }
        return this.messageSource;
    }

    @Nullable
    protected MessageSource getInternalParentMessageSource() {
        return (getParent() instanceof AbstractApplicationContext ? ((AbstractApplicationContext) getParent()).messageSource : getParent());
    }
    //---------------------------------------------------------------------
    // Implementation of ResourcePatternResolver interface
    //---------------------------------------------------------------------

    @Override
    public Resource[] getResources(String locationPattern) throws IOException {
        return this.resourcePatternResolver.getResources(locationPattern);
    }
    //---------------------------------------------------------------------
    // Implementation of Lifecycle interface
    //---------------------------------------------------------------------

    @Override
    public void start() {
        getLifecycleProcessor().start();
        publishEvent(new ContextStartedEvent(this));
    }

    @Override
    public void stop() {
        getLifecycleProcessor().stop();
        publishEvent(new ContextStoppedEvent(this));
    }

    @Override
    public boolean isRunning() {
        return (this.lifecycleProcessor != null && this.lifecycleProcessor.isRunning());
    }
    //---------------------------------------------------------------------
    // Abstract methods that must be implemented by subclasses
    //---------------------------------------------------------------------

    /**
     * AbstractRefreshableApplicationContext#refreshBeanFactory
     * GenericApplicationContext#refreshBeanFactory
     */
    protected abstract void refreshBeanFactory() throws BeansException, IllegalStateException;

    protected abstract void closeBeanFactory();

    @Override
    public abstract ConfigurableListableBeanFactory getBeanFactory() throws IllegalStateException;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getDisplayName());
        sb.append(", started on ").append(new Date(getStartupDate()));
        ApplicationContext parent = getParent();
        if (parent != null) {
            sb.append(", parent: ").append(parent.getDisplayName());
        }
        return sb.toString();
    }

}
