package org.springframework.beans.factory.support;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.config.*;
import org.springframework.core.OrderComparator;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.lang.Nullable;
import org.springframework.util.*;

import javax.inject.Provider;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

@SuppressWarnings("serial")
public class DefaultListableBeanFactory extends AbstractAutowireCapableBeanFactory implements ConfigurableListableBeanFactory, BeanDefinitionRegistry, Serializable {

    @Nullable
    private static Class<?> javaxInjectProviderClass;

    static {
        try {
            javaxInjectProviderClass = ClassUtils.forName("javax.inject.Provider", DefaultListableBeanFactory.class.getClassLoader());
        } catch (ClassNotFoundException ex) {
            // JSR-330 API not available - Provider interface simply not supported then.
            javaxInjectProviderClass = null;
        }
    }

    private static final Map<String, Reference<DefaultListableBeanFactory>> serializableFactories = new ConcurrentHashMap<>(8);

    @Nullable
    private String serializationId;

    private boolean allowBeanDefinitionOverriding = true;

    private boolean allowEagerClassLoading = true;

    @Nullable
    private Comparator<Object> dependencyComparator;

    private AutowireCandidateResolver autowireCandidateResolver = new SimpleAutowireCandidateResolver();

    private final Map<Class<?>, Object> resolvableDependencies = new ConcurrentHashMap<>(16);

    private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(256);

    private final Map<Class<?>, String[]> allBeanNamesByType = new ConcurrentHashMap<>(64);

    private final Map<Class<?>, String[]> singletonBeanNamesByType = new ConcurrentHashMap<>(64);

    private volatile List<String> beanDefinitionNames = new ArrayList<>(256);

    private volatile Set<String> manualSingletonNames = new LinkedHashSet<>(16);

    @Nullable
    private volatile String[] frozenBeanDefinitionNames;

    private volatile boolean configurationFrozen = false;

    public DefaultListableBeanFactory() {
        super();
    }

    public DefaultListableBeanFactory(@Nullable BeanFactory parentBeanFactory) {
        super(parentBeanFactory);
    }

    public void setSerializationId(@Nullable String serializationId) {
        if (serializationId != null) {
            serializableFactories.put(serializationId, new WeakReference<>(this));
        } else if (this.serializationId != null) {
            serializableFactories.remove(this.serializationId);
        }
        this.serializationId = serializationId;
    }

    @Nullable
    public String getSerializationId() {
        return this.serializationId;
    }

    public void setAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
        this.allowBeanDefinitionOverriding = allowBeanDefinitionOverriding;
    }

    public boolean isAllowBeanDefinitionOverriding() {
        return this.allowBeanDefinitionOverriding;
    }

    public void setAllowEagerClassLoading(boolean allowEagerClassLoading) {
        this.allowEagerClassLoading = allowEagerClassLoading;
    }

    public boolean isAllowEagerClassLoading() {
        return this.allowEagerClassLoading;
    }

    public void setDependencyComparator(@Nullable Comparator<Object> dependencyComparator) {
        this.dependencyComparator = dependencyComparator;
    }

    @Nullable
    public Comparator<Object> getDependencyComparator() {
        return this.dependencyComparator;
    }

    public void setAutowireCandidateResolver(final AutowireCandidateResolver autowireCandidateResolver) {
        Assert.notNull(autowireCandidateResolver, "AutowireCandidateResolver must not be null");
        if (autowireCandidateResolver instanceof BeanFactoryAware) {
            if (System.getSecurityManager() != null) {
                AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
                    ((BeanFactoryAware) autowireCandidateResolver).setBeanFactory(DefaultListableBeanFactory.this);
                    return null;
                }, getAccessControlContext());
            } else {
                ((BeanFactoryAware) autowireCandidateResolver).setBeanFactory(this);
            }
        }
        this.autowireCandidateResolver = autowireCandidateResolver;
    }

    public AutowireCandidateResolver getAutowireCandidateResolver() {
        return this.autowireCandidateResolver;
    }

    @Override
    public void copyConfigurationFrom(ConfigurableBeanFactory otherFactory) {
        super.copyConfigurationFrom(otherFactory);
        if (otherFactory instanceof DefaultListableBeanFactory) {
            DefaultListableBeanFactory otherListableFactory = (DefaultListableBeanFactory) otherFactory;
            this.allowBeanDefinitionOverriding = otherListableFactory.allowBeanDefinitionOverriding;
            this.allowEagerClassLoading = otherListableFactory.allowEagerClassLoading;
            this.dependencyComparator = otherListableFactory.dependencyComparator;
            // A clone of the AutowireCandidateResolver since it is potentially BeanFactoryAware...
            setAutowireCandidateResolver(BeanUtils.instantiateClass(getAutowireCandidateResolver().getClass()));
            // Make resolvable dependencies (e.g. ResourceLoader) available here as well...
            this.resolvableDependencies.putAll(otherListableFactory.resolvableDependencies);
        }
    }
    //---------------------------------------------------------------------
    // Implementation of remaining BeanFactory methods
    //---------------------------------------------------------------------

    @Override
    public <T> T getBean(Class<T> requiredType) throws BeansException {
        return getBean(requiredType, (Object[]) null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getBean(Class<T> requiredType, @Nullable Object... args) throws BeansException {
        Object resolved = resolveBean(ResolvableType.forRawClass(requiredType), args, false);
        if (resolved == null) {
            throw new NoSuchBeanDefinitionException(requiredType);
        }
        return (T) resolved;
    }

    @Override
    public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType) throws BeansException {
        return getBeanProvider(ResolvableType.forRawClass(requiredType));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType) {
        return new BeanObjectProvider<T>() {
            @Override
            public T getObject() throws BeansException {
                T resolved = resolveBean(requiredType, null, false);
                if (resolved == null) {
                    throw new NoSuchBeanDefinitionException(requiredType);
                }
                return resolved;
            }

            @Override
            public T getObject(Object... args) throws BeansException {
                T resolved = resolveBean(requiredType, args, false);
                if (resolved == null) {
                    throw new NoSuchBeanDefinitionException(requiredType);
                }
                return resolved;
            }

            @Override
            @Nullable
            public T getIfAvailable() throws BeansException {
                return resolveBean(requiredType, null, false);
            }

            @Override
            @Nullable
            public T getIfUnique() throws BeansException {
                return resolveBean(requiredType, null, true);
            }

            @Override
            public Stream<T> stream() {
                return Arrays.stream(getBeanNamesForTypedStream(requiredType)).map(name -> (T) getBean(name)).filter(bean -> !(bean instanceof NullBean));
            }

            @Override
            public Stream<T> orderedStream() {
                String[] beanNames = getBeanNamesForTypedStream(requiredType);
                Map<String, T> matchingBeans = new LinkedHashMap<>(beanNames.length);
                for (String beanName : beanNames) {
                    Object beanInstance = getBean(beanName);
                    if (!(beanInstance instanceof NullBean)) {
                        matchingBeans.put(beanName, (T) beanInstance);
                    }
                }
                Stream<T> stream = matchingBeans.values().stream();
                return stream.sorted(adaptOrderComparator(matchingBeans));
            }
        };
    }

    @Nullable
    private <T> T resolveBean(ResolvableType requiredType, @Nullable Object[] args, boolean nonUniqueAsNull) {
        NamedBeanHolder<T> namedBean = resolveNamedBean(requiredType, args, nonUniqueAsNull);
        if (namedBean != null) {
            return namedBean.getBeanInstance();
        }
        BeanFactory parent = getParentBeanFactory();
        if (parent instanceof DefaultListableBeanFactory) {
            return ((DefaultListableBeanFactory) parent).resolveBean(requiredType, args, nonUniqueAsNull);
        } else if (parent != null) {
            ObjectProvider<T> parentProvider = parent.getBeanProvider(requiredType);
            if (args != null) {
                return parentProvider.getObject(args);
            } else {
                return (nonUniqueAsNull ? parentProvider.getIfUnique() : parentProvider.getIfAvailable());
            }
        }
        return null;
    }

    private String[] getBeanNamesForTypedStream(ResolvableType requiredType) {
        return BeanFactoryUtils.beanNamesForTypeIncludingAncestors(this, requiredType);
    }
    //---------------------------------------------------------------------
    // Implementation of ListableBeanFactory interface
    //---------------------------------------------------------------------

    @Override
    public boolean containsBeanDefinition(String beanName) {
        Assert.notNull(beanName, "Bean name must not be null");
        return this.beanDefinitionMap.containsKey(beanName);
    }

    @Override
    public int getBeanDefinitionCount() {
        return this.beanDefinitionMap.size();
    }

    @Override
    public String[] getBeanDefinitionNames() {
        String[] frozenNames = this.frozenBeanDefinitionNames;
        if (frozenNames != null) {
            return frozenNames.clone();
        } else {
            return StringUtils.toStringArray(this.beanDefinitionNames);
        }
    }

    @Override
    public String[] getBeanNamesForType(ResolvableType type) {
        Class<?> resolved = type.resolve();
        if (resolved != null && !type.hasGenerics()) {
            return getBeanNamesForType(resolved, true, true);
        } else {
            return doGetBeanNamesForType(type, true, true);
        }
    }

    @Override
    public String[] getBeanNamesForType(@Nullable Class<?> type) {
        return getBeanNamesForType(type, true, true);
    }

    @Override
    public String[] getBeanNamesForType(@Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
        if (!isConfigurationFrozen() || type == null || !allowEagerInit) {
            return doGetBeanNamesForType(ResolvableType.forRawClass(type), includeNonSingletons, allowEagerInit);
        }
        Map<Class<?>, String[]> cache = (includeNonSingletons ? this.allBeanNamesByType : this.singletonBeanNamesByType);
        String[] resolvedBeanNames = cache.get(type);
        if (resolvedBeanNames != null) {
            return resolvedBeanNames;
        }
        resolvedBeanNames = doGetBeanNamesForType(ResolvableType.forRawClass(type), includeNonSingletons, true);
        if (ClassUtils.isCacheSafe(type, getBeanClassLoader())) {
            cache.put(type, resolvedBeanNames);
        }
        return resolvedBeanNames;
    }

    /**
     * 检查beanDefinitionNames
     * 检查manualSingletonNames
     */
    private String[] doGetBeanNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {
        List<String> result = new ArrayList<>();
        // Check all bean definitions.
        for (String beanName : this.beanDefinitionNames) {
            // 不能是别名
            if (!isAlias(beanName)) {
                try {
                    RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
                    // Only check bean definition if it is complete.
                    if (!mbd.isAbstract() && (allowEagerInit || (mbd.hasBeanClass() || !mbd.isLazyInit() || isAllowEagerClassLoading()) && !requiresEagerInitForType(mbd.getFactoryBeanName()))) {
                        // In case of FactoryBean, match object created by FactoryBean.
                        boolean isFactoryBean = isFactoryBean(beanName, mbd);
                        BeanDefinitionHolder dbd = mbd.getDecoratedDefinition();
                        boolean matchFound = (allowEagerInit || !isFactoryBean || (dbd != null && !mbd.isLazyInit()) || containsSingleton(beanName)) && (includeNonSingletons || (dbd != null ? mbd.isSingleton() : isSingleton(beanName))) && isTypeMatch(beanName, type);
                        // 不匹配 && Bean是FactoryBean
                        if (!matchFound && isFactoryBean) {
                            // '&' + beanName，找FactoryBean
                            beanName = FACTORY_BEAN_PREFIX + beanName;
                            matchFound = (includeNonSingletons || mbd.isSingleton()) && isTypeMatch(beanName, type);
                        }
                        // 匹配加入
                        if (matchFound) {
                            result.add(beanName);
                        }
                    }
                } catch (CannotLoadBeanClassException ex) {
                    if (allowEagerInit) {
                        throw ex;
                    }
                    // Probably a class name with a placeholder: let's ignore it for type matching purposes.
                    if (logger.isTraceEnabled()) {
                        logger.trace("Ignoring bean class loading failure for bean '" + beanName + "'", ex);
                    }
                    onSuppressedException(ex);
                } catch (BeanDefinitionStoreException ex) {
                    if (allowEagerInit) {
                        throw ex;
                    }
                    // Probably some metadata with a placeholder: let's ignore it for type matching purposes.
                    if (logger.isTraceEnabled()) {
                        logger.trace("Ignoring unresolvable metadata in bean definition '" + beanName + "'", ex);
                    }
                    onSuppressedException(ex);
                }
            }
        }
        // environment，systemProperties，systemEnvironment
        for (String beanName : this.manualSingletonNames) {
            try {
                // In case of FactoryBean, match object created by FactoryBean.
                if (isFactoryBean(beanName)) {
                    if ((includeNonSingletons || isSingleton(beanName)) && isTypeMatch(beanName, type)) {
                        result.add(beanName);
                        // Match found for this bean: do not match FactoryBean itself anymore.
                        continue;
                    }
                    // In case of FactoryBean, try to match FactoryBean itself next.
                    beanName = FACTORY_BEAN_PREFIX + beanName;
                }
                // Match raw bean instance (might be raw FactoryBean).
                if (isTypeMatch(beanName, type)) {
                    result.add(beanName);
                }
            } catch (NoSuchBeanDefinitionException ex) {
                // Shouldn't happen - probably a result of circular reference resolution...
                if (logger.isTraceEnabled()) {
                    logger.trace("Failed to check manually registered singleton with name '" + beanName + "'", ex);
                }
            }
        }
        return StringUtils.toStringArray(result);
    }

    private boolean requiresEagerInitForType(@Nullable String factoryBeanName) {
        return (factoryBeanName != null && isFactoryBean(factoryBeanName) && !containsSingleton(factoryBeanName));
    }

    @Override
    public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type) throws BeansException {
        return getBeansOfType(type, true, true);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type, boolean includeNonSingletons, boolean allowEagerInit) throws BeansException {
        String[] beanNames = getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
        Map<String, T> result = new LinkedHashMap<>(beanNames.length);
        for (String beanName : beanNames) {
            try {
                Object beanInstance = getBean(beanName);
                if (!(beanInstance instanceof NullBean)) {
                    result.put(beanName, (T) beanInstance);
                }
            } catch (BeanCreationException ex) {
                Throwable rootCause = ex.getMostSpecificCause();
                if (rootCause instanceof BeanCurrentlyInCreationException) {
                    BeanCreationException bce = (BeanCreationException) rootCause;
                    String exBeanName = bce.getBeanName();
                    if (exBeanName != null && isCurrentlyInCreation(exBeanName)) {
                        if (logger.isTraceEnabled()) {
                            logger.trace("Ignoring match to currently created bean '" + exBeanName + "': " + ex.getMessage());
                        }
                        onSuppressedException(ex);
                        // Ignore: indicates a circular reference when autowiring constructors.
                        // We want to find matches other than the currently created bean itself.
                        continue;
                    }
                }
                throw ex;
            }
        }
        return result;
    }

    @Override
    public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
        List<String> result = new ArrayList<>();
        for (String beanName : this.beanDefinitionNames) {
            BeanDefinition beanDefinition = getBeanDefinition(beanName);
            if (!beanDefinition.isAbstract() && findAnnotationOnBean(beanName, annotationType) != null) {
                result.add(beanName);
            }
        }
        for (String beanName : this.manualSingletonNames) {
            if (!result.contains(beanName) && findAnnotationOnBean(beanName, annotationType) != null) {
                result.add(beanName);
            }
        }
        return StringUtils.toStringArray(result);
    }

    @Override
    public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) {
        String[] beanNames = getBeanNamesForAnnotation(annotationType);
        Map<String, Object> result = new LinkedHashMap<>(beanNames.length);
        for (String beanName : beanNames) {
            Object beanInstance = getBean(beanName);
            if (!(beanInstance instanceof NullBean)) {
                result.put(beanName, beanInstance);
            }
        }
        return result;
    }

    @Override
    @Nullable
    public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType) throws NoSuchBeanDefinitionException {
        return findMergedAnnotationOnBean(beanName, annotationType).synthesize(MergedAnnotation::isPresent).orElse(null);
    }

    private <A extends Annotation> MergedAnnotation<A> findMergedAnnotationOnBean(String beanName, Class<A> annotationType) {
        Class<?> beanType = getType(beanName);
        if (beanType != null) {
            MergedAnnotation<A> annotation = MergedAnnotations.from(beanType, SearchStrategy.EXHAUSTIVE).get(annotationType);
            if (annotation.isPresent()) {
                return annotation;
            }
        }
        if (containsBeanDefinition(beanName)) {
            RootBeanDefinition bd = getMergedLocalBeanDefinition(beanName);
            // Check raw bean class, e.g. in case of a proxy.
            if (bd.hasBeanClass()) {
                Class<?> beanClass = bd.getBeanClass();
                if (beanClass != beanType) {
                    MergedAnnotation<A> annotation = MergedAnnotations.from(beanClass, SearchStrategy.EXHAUSTIVE).get(annotationType);
                    if (annotation.isPresent()) {
                        return annotation;
                    }
                }
            }
            // Check annotations declared on factory method, if any.
            Method factoryMethod = bd.getResolvedFactoryMethod();
            if (factoryMethod != null) {
                MergedAnnotation<A> annotation = MergedAnnotations.from(factoryMethod, SearchStrategy.EXHAUSTIVE).get(annotationType);
                if (annotation.isPresent()) {
                    return annotation;
                }
            }
        }
        return MergedAnnotation.missing();
    }
    //---------------------------------------------------------------------
    // Implementation of ConfigurableListableBeanFactory interface
    //---------------------------------------------------------------------

    @Override
    public void registerResolvableDependency(Class<?> dependencyType, @Nullable Object autowiredValue) {
        Assert.notNull(dependencyType, "Dependency type must not be null");
        if (autowiredValue != null) {
            if (!(autowiredValue instanceof ObjectFactory || dependencyType.isInstance(autowiredValue))) {
                throw new IllegalArgumentException("Value [" + autowiredValue + "] does not implement specified dependency type [" + dependencyType.getName() + "]");
            }
            this.resolvableDependencies.put(dependencyType, autowiredValue);
        }
    }

    @Override
    public boolean isAutowireCandidate(String beanName, DependencyDescriptor descriptor) throws NoSuchBeanDefinitionException {
        return isAutowireCandidate(beanName, descriptor, getAutowireCandidateResolver());
    }

    protected boolean isAutowireCandidate(String beanName, DependencyDescriptor descriptor, AutowireCandidateResolver resolver) throws NoSuchBeanDefinitionException {
        String beanDefinitionName = BeanFactoryUtils.transformedBeanName(beanName);
        if (containsBeanDefinition(beanDefinitionName)) {
            return isAutowireCandidate(beanName, getMergedLocalBeanDefinition(beanDefinitionName), descriptor, resolver);
        } else if (containsSingleton(beanName)) {
            return isAutowireCandidate(beanName, new RootBeanDefinition(getType(beanName)), descriptor, resolver);
        }
        BeanFactory parent = getParentBeanFactory();
        if (parent instanceof DefaultListableBeanFactory) {
            // No bean definition found in this factory -> delegate to parent.
            return ((DefaultListableBeanFactory) parent).isAutowireCandidate(beanName, descriptor, resolver);
        } else if (parent instanceof ConfigurableListableBeanFactory) {
            // If no DefaultListableBeanFactory, can't pass the resolver along.
            return ((ConfigurableListableBeanFactory) parent).isAutowireCandidate(beanName, descriptor);
        } else {
            return true;
        }
    }

    protected boolean isAutowireCandidate(String beanName, RootBeanDefinition mbd, DependencyDescriptor descriptor, AutowireCandidateResolver resolver) {
        String beanDefinitionName = BeanFactoryUtils.transformedBeanName(beanName);
        resolveBeanClass(mbd, beanDefinitionName);
        if (mbd.isFactoryMethodUnique && mbd.factoryMethodToIntrospect == null) {
            new ConstructorResolver(this).resolveFactoryMethodIfPossible(mbd);
        }
        return resolver.isAutowireCandidate(new BeanDefinitionHolder(mbd, beanName, getAliases(beanDefinitionName)), descriptor);
    }

    @Override
    public BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
        BeanDefinition bd = this.beanDefinitionMap.get(beanName);
        if (bd == null) {
            if (logger.isTraceEnabled()) {
                logger.trace("No bean named '" + beanName + "' found in " + this);
            }
            throw new NoSuchBeanDefinitionException(beanName);
        }
        return bd;
    }

    @Override
    public Iterator<String> getBeanNamesIterator() {
        CompositeIterator<String> iterator = new CompositeIterator<>();
        iterator.add(this.beanDefinitionNames.iterator());
        iterator.add(this.manualSingletonNames.iterator());
        return iterator;
    }

    @Override
    public void clearMetadataCache() {
        super.clearMetadataCache();
        clearByTypeCache();
    }

    @Override
    public void freezeConfiguration() {
        this.configurationFrozen = true;
        this.frozenBeanDefinitionNames = StringUtils.toStringArray(this.beanDefinitionNames);
    }

    @Override
    public boolean isConfigurationFrozen() {
        return this.configurationFrozen;
    }

    @Override
    protected boolean isBeanEligibleForMetadataCaching(String beanName) {
        return (this.configurationFrozen || super.isBeanEligibleForMetadataCaching(beanName));
    }

    /**
     * 两种类型的Bean：普通JavaBean，FactoryBean
     * FactoryBean是实现了FactoryBean<T>接口的Bean，通过getBean方法获取到的不是该FactoryBean的实例，而是FactoryBean#getObject返回的对象
     * 可以通过getBean方法获取实例时在参数name前面加上“&”获取该FactoryBean的实例
     */
    @Override
    public void preInstantiateSingletons() throws BeansException {
        if (logger.isTraceEnabled()) {
            logger.trace("Pre-instantiating singletons in " + this);
        }
        // this.beanDefinitionNames保存了所有beanName
        List<String> beanNames = new ArrayList<>(this.beanDefinitionNames);
        // 遍历
        for (String beanName : beanNames) {
            // 合并父Bean中的配置，<bean id="" class="" parent=""/>
            RootBeanDefinition bd = getMergedLocalBeanDefinition(beanName);
            // 非抽象 && 单例 && 非懒加载
            if (!bd.isAbstract() && bd.isSingleton() && !bd.isLazyInit()) {
                // FactoryBean：从单例缓存或者BeanDefinition判断
                if (isFactoryBean(beanName)) {
                    // 加载FactoryBean，在beanName前加&，再getBean
                    Object bean = getBean(FACTORY_BEAN_PREFIX + beanName);
                    if (bean instanceof FactoryBean) {
                        final FactoryBean<?> factory = (FactoryBean<?>) bean;
                        // 当前FactoryBean是否是SmartFactoryBean的实现，检查是否需要提前加载
                        boolean isEagerInit;
                        if (System.getSecurityManager() != null && factory instanceof SmartFactoryBean) {
                            isEagerInit = AccessController.doPrivileged((PrivilegedAction<Boolean>) ((SmartFactoryBean<?>) factory)::isEagerInit, getAccessControlContext());
                        } else {
                            isEagerInit = (factory instanceof SmartFactoryBean && ((SmartFactoryBean<?>) factory).isEagerInit());
                        }
                        // 提前加载
                        if (isEagerInit) {
                            // 加载FactoryBean生产的Bean
                            getBean(beanName);
                        }
                    }
                }
                // 普通的Bean初始化
                else {
                    getBean(beanName);
                }
            }
        }
        // 到这里所有非懒加载的单例Bean已经完成初始化，再次遍历
        for (String beanName : beanNames) {
            // 从singletonObjects获取单例
            Object singletonInstance = getSingleton(beanName);
            // 如果Bean实现了SmartInitializingSingleton接口，触发afterSingletonsInstantiated回调
            if (singletonInstance instanceof SmartInitializingSingleton) {
                final SmartInitializingSingleton smartSingleton = (SmartInitializingSingleton) singletonInstance;
                if (System.getSecurityManager() != null) {
                    AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
                        smartSingleton.afterSingletonsInstantiated();
                        return null;
                    }, getAccessControlContext());
                } else {
                    smartSingleton.afterSingletonsInstantiated();
                }
            }
        }
    }
    //---------------------------------------------------------------------
    // Implementation of BeanDefinitionRegistry interface
    //---------------------------------------------------------------------

    /**
     * 注册到工厂
     */
    @Override
    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) throws BeanDefinitionStoreException {
        Assert.hasText(beanName, "Bean name must not be empty");
        Assert.notNull(beanDefinition, "BeanDefinition must not be null");
        if (beanDefinition instanceof AbstractBeanDefinition) {
            try {
                ((AbstractBeanDefinition) beanDefinition).validate();
            } catch (BeanDefinitionValidationException ex) {
                throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription(), beanName, "Validation of bean definition failed", ex);
            }
        }
        // 重名的BeanDefinition
        BeanDefinition existingDefinition = this.beanDefinitionMap.get(beanName);
        // 有重复名称的Bean
        if (existingDefinition != null) {
            // 如果不允许覆盖，抛异常
            if (!isAllowBeanDefinitionOverriding()) {
                throw new BeanDefinitionOverrideException(beanName, beanDefinition, existingDefinition);
            }
            // 日志，用框架生成的Bean覆盖用户Bean
            else if (existingDefinition.getRole() < beanDefinition.getRole()) {
                // e.g. was ROLE_APPLICATION, now overriding with ROLE_SUPPORT or ROLE_INFRASTRUCTURE
                if (logger.isInfoEnabled()) {
                    logger.info("Overriding user-defined bean definition for bean '" + beanName + "' with a framework-generated bean definition: replacing [" + existingDefinition + "] with [" + beanDefinition + "]");
                }
            }
            // 日志，用新的Bean覆盖旧的Bean
            else if (!beanDefinition.equals(existingDefinition)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Overriding bean definition for bean '" + beanName + "' with a different definition: replacing [" + existingDefinition + "] with [" + beanDefinition + "]");
                }
            }
            // 日志，用equals的Bean覆盖旧的Bean
            else {
                if (logger.isTraceEnabled()) {
                    logger.trace("Overriding bean definition for bean '" + beanName + "' with an equivalent definition: replacing [" + existingDefinition + "] with [" + beanDefinition + "]");
                }
            }
            // 覆盖
            this.beanDefinitionMap.put(beanName, beanDefinition);
        }
        // 没有重名的Bean
        else {
            // 如果已经有其他的Bean开始初始化了
            if (hasBeanCreationStarted()) {
                // Cannot modify startup-time collection elements anymore (for stable iteration)
                synchronized (this.beanDefinitionMap) {
                    this.beanDefinitionMap.put(beanName, beanDefinition);
                    List<String> updatedDefinitions = new ArrayList<>(this.beanDefinitionNames.size() + 1);
                    updatedDefinitions.addAll(this.beanDefinitionNames);
                    updatedDefinitions.add(beanName);
                    this.beanDefinitionNames = updatedDefinitions;
                    removeManualSingletonName(beanName);
                }
            }
            // 正常分支，将BeanDefinition放入映射
            else {
                // Still in startup registration phase
                this.beanDefinitionMap.put(beanName, beanDefinition);
                this.beanDefinitionNames.add(beanName);

                removeManualSingletonName(beanName);
            }
            this.frozenBeanDefinitionNames = null;
        }
        if (existingDefinition != null || containsSingleton(beanName)) {
            resetBeanDefinition(beanName);
        }
    }

    @Override
    public void removeBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
        Assert.hasText(beanName, "'beanName' must not be empty");
        BeanDefinition bd = this.beanDefinitionMap.remove(beanName);
        if (bd == null) {
            if (logger.isTraceEnabled()) {
                logger.trace("No bean named '" + beanName + "' found in " + this);
            }
            throw new NoSuchBeanDefinitionException(beanName);
        }
        if (hasBeanCreationStarted()) {
            // Cannot modify startup-time collection elements anymore (for stable iteration)
            synchronized (this.beanDefinitionMap) {
                List<String> updatedDefinitions = new ArrayList<>(this.beanDefinitionNames);
                updatedDefinitions.remove(beanName);
                this.beanDefinitionNames = updatedDefinitions;
            }
        } else {
            // Still in startup registration phase
            this.beanDefinitionNames.remove(beanName);
        }
        this.frozenBeanDefinitionNames = null;
        resetBeanDefinition(beanName);
    }

    protected void resetBeanDefinition(String beanName) {
        // Remove the merged bean definition for the given bean, if already created.
        clearMergedBeanDefinition(beanName);
        // Remove corresponding bean from singleton cache, if any. Shouldn't usually
        // be necessary, rather just meant for overriding a context's default beans
        // (e.g. the default StaticMessageSource in a StaticApplicationContext).
        destroySingleton(beanName);
        // Notify all post-processors that the specified bean definition has been reset.
        for (BeanPostProcessor processor : getBeanPostProcessors()) {
            if (processor instanceof MergedBeanDefinitionPostProcessor) {
                ((MergedBeanDefinitionPostProcessor) processor).resetBeanDefinition(beanName);
            }
        }
        // Reset all bean definitions that have the given bean as parent (recursively).
        for (String bdName : this.beanDefinitionNames) {
            if (!beanName.equals(bdName)) {
                BeanDefinition bd = this.beanDefinitionMap.get(bdName);
                if (beanName.equals(bd.getParentName())) {
                    resetBeanDefinition(bdName);
                }
            }
        }
    }

    @Override
    protected boolean allowAliasOverriding() {
        return isAllowBeanDefinitionOverriding();
    }

    @Override
    public void registerSingleton(String beanName, Object singletonObject) throws IllegalStateException {
        super.registerSingleton(beanName, singletonObject);
        updateManualSingletonNames(set -> set.add(beanName), set -> !this.beanDefinitionMap.containsKey(beanName));
        clearByTypeCache();
    }

    @Override
    public void destroySingletons() {
        super.destroySingletons();
        updateManualSingletonNames(Set::clear, set -> !set.isEmpty());
        clearByTypeCache();
    }

    @Override
    public void destroySingleton(String beanName) {
        super.destroySingleton(beanName);
        removeManualSingletonName(beanName);
        clearByTypeCache();
    }

    private void removeManualSingletonName(String beanName) {
        updateManualSingletonNames(set -> set.remove(beanName), set -> set.contains(beanName));
    }

    private void updateManualSingletonNames(Consumer<Set<String>> action, Predicate<Set<String>> condition) {
        if (hasBeanCreationStarted()) {
            // Cannot modify startup-time collection elements anymore (for stable iteration)
            synchronized (this.beanDefinitionMap) {
                if (condition.test(this.manualSingletonNames)) {
                    Set<String> updatedSingletons = new LinkedHashSet<>(this.manualSingletonNames);
                    action.accept(updatedSingletons);
                    this.manualSingletonNames = updatedSingletons;
                }
            }
        } else {
            // Still in startup registration phase
            if (condition.test(this.manualSingletonNames)) {
                action.accept(this.manualSingletonNames);
            }
        }
    }

    private void clearByTypeCache() {
        this.allBeanNamesByType.clear();
        this.singletonBeanNamesByType.clear();
    }
    //---------------------------------------------------------------------
    // Dependency resolution functionality
    //---------------------------------------------------------------------

    @Override
    public <T> NamedBeanHolder<T> resolveNamedBean(Class<T> requiredType) throws BeansException {
        NamedBeanHolder<T> namedBean = resolveNamedBean(ResolvableType.forRawClass(requiredType), null, false);
        if (namedBean != null) {
            return namedBean;
        }
        BeanFactory parent = getParentBeanFactory();
        if (parent instanceof AutowireCapableBeanFactory) {
            return ((AutowireCapableBeanFactory) parent).resolveNamedBean(requiredType);
        }
        throw new NoSuchBeanDefinitionException(requiredType);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private <T> NamedBeanHolder<T> resolveNamedBean(ResolvableType requiredType, @Nullable Object[] args, boolean nonUniqueAsNull) throws BeansException {
        Assert.notNull(requiredType, "Required type must not be null");
        String[] candidateNames = getBeanNamesForType(requiredType);
        if (candidateNames.length > 1) {
            List<String> autowireCandidates = new ArrayList<>(candidateNames.length);
            for (String beanName : candidateNames) {
                if (!containsBeanDefinition(beanName) || getBeanDefinition(beanName).isAutowireCandidate()) {
                    autowireCandidates.add(beanName);
                }
            }
            if (!autowireCandidates.isEmpty()) {
                candidateNames = StringUtils.toStringArray(autowireCandidates);
            }
        }
        if (candidateNames.length == 1) {
            String beanName = candidateNames[0];
            return new NamedBeanHolder<>(beanName, (T) getBean(beanName, requiredType.toClass(), args));
        } else if (candidateNames.length > 1) {
            Map<String, Object> candidates = new LinkedHashMap<>(candidateNames.length);
            for (String beanName : candidateNames) {
                if (containsSingleton(beanName) && args == null) {
                    Object beanInstance = getBean(beanName);
                    candidates.put(beanName, (beanInstance instanceof NullBean ? null : beanInstance));
                } else {
                    candidates.put(beanName, getType(beanName));
                }
            }
            String candidateName = determinePrimaryCandidate(candidates, requiredType.toClass());
            if (candidateName == null) {
                candidateName = determineHighestPriorityCandidate(candidates, requiredType.toClass());
            }
            if (candidateName != null) {
                Object beanInstance = candidates.get(candidateName);
                if (beanInstance == null || beanInstance instanceof Class) {
                    beanInstance = getBean(candidateName, requiredType.toClass(), args);
                }
                return new NamedBeanHolder<>(candidateName, (T) beanInstance);
            }
            if (!nonUniqueAsNull) {
                throw new NoUniqueBeanDefinitionException(requiredType, candidates.keySet());
            }
        }
        return null;
    }

    @Override
    @Nullable
    public Object resolveDependency(DependencyDescriptor descriptor, @Nullable String requestingBeanName, @Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) throws BeansException {
        descriptor.initParameterNameDiscovery(getParameterNameDiscoverer());
        if (Optional.class == descriptor.getDependencyType()) {
            return createOptionalDependency(descriptor, requestingBeanName);
        } else if (ObjectFactory.class == descriptor.getDependencyType() || ObjectProvider.class == descriptor.getDependencyType()) {
            return new DependencyObjectProvider(descriptor, requestingBeanName);
        } else if (javaxInjectProviderClass == descriptor.getDependencyType()) {
            return new Jsr330Factory().createDependencyProvider(descriptor, requestingBeanName);
        } else {
            Object result = getAutowireCandidateResolver().getLazyResolutionProxyIfNecessary(descriptor, requestingBeanName);
            if (result == null) {
                result = doResolveDependency(descriptor, requestingBeanName, autowiredBeanNames, typeConverter);
            }
            return result;
        }
    }

    @Nullable
    public Object doResolveDependency(DependencyDescriptor descriptor, @Nullable String beanName, @Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) throws BeansException {
        InjectionPoint previousInjectionPoint = ConstructorResolver.setCurrentInjectionPoint(descriptor);
        try {
            // 先查缓存
            Object shortcut = descriptor.resolveShortcut(this);
            if (shortcut != null) {
                return shortcut;
            }
            // 参数值类型
            Class<?> type = descriptor.getDependencyType();
            // 获取@Value("${xxx}")的参数${xxx}
            Object value = getAutowireCandidateResolver().getSuggestedValue(descriptor);
            if (value != null) {
                // String类型的注入
                if (value instanceof String) {
                    // 从配置文件获取属性值
                    String strVal = resolveEmbeddedValue((String) value);
                    BeanDefinition bd = (beanName != null && containsBean(beanName) ? getMergedBeanDefinition(beanName) : null);
                    // 解析
                    value = evaluateBeanDefinitionString(strVal, bd);
                }

                TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
                try {
                    // 类型转换，String转需要的类型
                    return converter.convertIfNecessary(value, type, descriptor.getTypeDescriptor());
                } catch (UnsupportedOperationException ex) {
                    // A custom TypeConverter which does not support TypeDescriptor resolution...
                    return (descriptor.getField() != null ? converter.convertIfNecessary(value, type, descriptor.getField()) : converter.convertIfNecessary(value, type, descriptor.getMethodParameter()));
                }
            }

            // Array，Collection，Map类型的注入
            Object multipleBeans = resolveMultipleBeans(descriptor, beanName, autowiredBeanNames, typeConverter);
            if (multipleBeans != null) {
                return multipleBeans;
            }
            // 一般Bean的注入
            Map<String, Object> matchingBeans = findAutowireCandidates(beanName, type, descriptor);
            if (matchingBeans.isEmpty()) {
                if (isRequired(descriptor)) {
                    raiseNoMatchingBeanFound(type, descriptor.getResolvableType(), descriptor);
                }
                return null;
            }
            String autowiredBeanName;
            Object instanceCandidate;
            // 单个Bean依赖却找到了多个候选，参考@Primary，@Priority
            if (matchingBeans.size() > 1) {
                autowiredBeanName = determineAutowireCandidate(matchingBeans, descriptor);
                if (autowiredBeanName == null) {
                    if (isRequired(descriptor) || !indicatesMultipleBeans(type)) {
                        return descriptor.resolveNotUnique(descriptor.getResolvableType(), matchingBeans);
                    } else {
                        // In case of an optional Collection/Map, silently ignore a non-unique case:
                        // possibly it was meant to be an empty collection of multiple regular beans
                        // (before 4.3 in particular when we didn't even look for collection beans).
                        return null;
                    }
                }
                instanceCandidate = matchingBeans.get(autowiredBeanName);
            }
            // 单个Bean依赖，单个候选
            else {
                // We have exactly one match.
                Map.Entry<String, Object> entry = matchingBeans.entrySet().iterator().next();
                autowiredBeanName = entry.getKey();
                instanceCandidate = entry.getValue();
            }
            if (autowiredBeanNames != null) {
                autowiredBeanNames.add(autowiredBeanName);
            }
            if (instanceCandidate instanceof Class) {
                instanceCandidate = descriptor.resolveCandidate(autowiredBeanName, type, this);
            }
            Object result = instanceCandidate;
            if (result instanceof NullBean) {
                if (isRequired(descriptor)) {
                    raiseNoMatchingBeanFound(type, descriptor.getResolvableType(), descriptor);
                }
                result = null;
            }
            if (!ClassUtils.isAssignableValue(type, result)) {
                throw new BeanNotOfRequiredTypeException(autowiredBeanName, type, instanceCandidate.getClass());
            }
            return result;
        } finally {
            // 缓存
            ConstructorResolver.setCurrentInjectionPoint(previousInjectionPoint);
        }
    }

    @Nullable
    private Object resolveMultipleBeans(DependencyDescriptor descriptor, @Nullable String beanName, @Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) {
        final Class<?> type = descriptor.getDependencyType();
        if (descriptor instanceof StreamDependencyDescriptor) {
            Map<String, Object> matchingBeans = findAutowireCandidates(beanName, type, descriptor);
            if (autowiredBeanNames != null) {
                autowiredBeanNames.addAll(matchingBeans.keySet());
            }
            Stream<Object> stream = matchingBeans.keySet().stream().map(name -> descriptor.resolveCandidate(name, type, this)).filter(bean -> !(bean instanceof NullBean));
            if (((StreamDependencyDescriptor) descriptor).isOrdered()) {
                stream = stream.sorted(adaptOrderComparator(matchingBeans));
            }
            return stream;
        } else if (type.isArray()) {
            Class<?> componentType = type.getComponentType();
            ResolvableType resolvableType = descriptor.getResolvableType();
            Class<?> resolvedArrayType = resolvableType.resolve(type);
            if (resolvedArrayType != type) {
                componentType = resolvableType.getComponentType().resolve();
            }
            if (componentType == null) {
                return null;
            }
            Map<String, Object> matchingBeans = findAutowireCandidates(beanName, componentType, new MultiElementDescriptor(descriptor));
            if (matchingBeans.isEmpty()) {
                return null;
            }
            if (autowiredBeanNames != null) {
                autowiredBeanNames.addAll(matchingBeans.keySet());
            }
            TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
            Object result = converter.convertIfNecessary(matchingBeans.values(), resolvedArrayType);
            if (result instanceof Object[]) {
                Comparator<Object> comparator = adaptDependencyComparator(matchingBeans);
                if (comparator != null) {
                    Arrays.sort((Object[]) result, comparator);
                }
            }
            return result;
        } else if (Collection.class.isAssignableFrom(type) && type.isInterface()) {
            Class<?> elementType = descriptor.getResolvableType().asCollection().resolveGeneric();
            if (elementType == null) {
                return null;
            }
            Map<String, Object> matchingBeans = findAutowireCandidates(beanName, elementType, new MultiElementDescriptor(descriptor));
            if (matchingBeans.isEmpty()) {
                return null;
            }
            if (autowiredBeanNames != null) {
                autowiredBeanNames.addAll(matchingBeans.keySet());
            }
            TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
            Object result = converter.convertIfNecessary(matchingBeans.values(), type);
            if (result instanceof List) {
                Comparator<Object> comparator = adaptDependencyComparator(matchingBeans);
                if (comparator != null) {
                    ((List<?>) result).sort(comparator);
                }
            }
            return result;
        } else if (Map.class == type) {
            ResolvableType mapType = descriptor.getResolvableType().asMap();
            Class<?> keyType = mapType.resolveGeneric(0);
            if (String.class != keyType) {
                return null;
            }
            Class<?> valueType = mapType.resolveGeneric(1);
            if (valueType == null) {
                return null;
            }
            Map<String, Object> matchingBeans = findAutowireCandidates(beanName, valueType, new MultiElementDescriptor(descriptor));
            if (matchingBeans.isEmpty()) {
                return null;
            }
            if (autowiredBeanNames != null) {
                autowiredBeanNames.addAll(matchingBeans.keySet());
            }
            return matchingBeans;
        } else {
            return null;
        }
    }

    private boolean isRequired(DependencyDescriptor descriptor) {
        return getAutowireCandidateResolver().isRequired(descriptor);
    }

    private boolean indicatesMultipleBeans(Class<?> type) {
        return (type.isArray() || (type.isInterface() && (Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type))));
    }

    @Nullable
    private Comparator<Object> adaptDependencyComparator(Map<String, ?> matchingBeans) {
        Comparator<Object> comparator = getDependencyComparator();
        if (comparator instanceof OrderComparator) {
            return ((OrderComparator) comparator).withSourceProvider(createFactoryAwareOrderSourceProvider(matchingBeans));
        } else {
            return comparator;
        }
    }

    private Comparator<Object> adaptOrderComparator(Map<String, ?> matchingBeans) {
        Comparator<Object> dependencyComparator = getDependencyComparator();
        OrderComparator comparator = (dependencyComparator instanceof OrderComparator ? (OrderComparator) dependencyComparator : OrderComparator.INSTANCE);
        return comparator.withSourceProvider(createFactoryAwareOrderSourceProvider(matchingBeans));
    }

    private OrderComparator.OrderSourceProvider createFactoryAwareOrderSourceProvider(Map<String, ?> beans) {
        IdentityHashMap<Object, String> instancesToBeanNames = new IdentityHashMap<>();
        beans.forEach((beanName, instance) -> instancesToBeanNames.put(instance, beanName));
        return new FactoryAwareOrderSourceProvider(instancesToBeanNames);
    }

    protected Map<String, Object> findAutowireCandidates(@Nullable String beanName, Class<?> requiredType, DependencyDescriptor descriptor) {
        // 给定类型的所有Bean名称
        String[] candidateNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(this, requiredType, true, descriptor.isEager());
        Map<String, Object> result = new LinkedHashMap<>(candidateNames.length);
        // 遍历
        for (Map.Entry<Class<?>, Object> classObjectEntry : this.resolvableDependencies.entrySet()) {
            Class<?> autowiringType = classObjectEntry.getKey();
            // requiredType继承了autowiringType
            if (autowiringType.isAssignableFrom(requiredType)) {
                Object autowiringValue = classObjectEntry.getValue();
                autowiringValue = AutowireUtils.resolveAutowiringValue(autowiringValue, requiredType);
                if (requiredType.isInstance(autowiringValue)) {
                    result.put(ObjectUtils.identityToString(autowiringValue), autowiringValue);
                    break;
                }
            }
        }
        for (String candidate : candidateNames) {
            if (!isSelfReference(beanName, candidate) && isAutowireCandidate(candidate, descriptor)) {
                addCandidateEntry(result, candidate, descriptor, requiredType);
            }
        }
        // 数组和集合类的依赖注入
        if (result.isEmpty()) {
            boolean multiple = indicatesMultipleBeans(requiredType);
            // Consider fallback matches if the first pass failed to find anything...
            DependencyDescriptor fallbackDescriptor = descriptor.forFallbackMatch();
            for (String candidate : candidateNames) {
                if (!isSelfReference(beanName, candidate) && isAutowireCandidate(candidate, fallbackDescriptor) && (!multiple || getAutowireCandidateResolver().hasQualifier(descriptor))) {
                    addCandidateEntry(result, candidate, descriptor, requiredType);
                }
            }
            if (result.isEmpty() && !multiple) {
                // Consider self references as a final pass...
                // but in the case of a dependency collection, not the very same bean itself.
                for (String candidate : candidateNames) {
                    if (isSelfReference(beanName, candidate) && (!(descriptor instanceof MultiElementDescriptor) || !beanName.equals(candidate)) && isAutowireCandidate(candidate, fallbackDescriptor)) {
                        addCandidateEntry(result, candidate, descriptor, requiredType);
                    }
                }
            }
        }
        return result;
    }

    private void addCandidateEntry(Map<String, Object> candidates, String candidateName, DependencyDescriptor descriptor, Class<?> requiredType) {
        if (descriptor instanceof MultiElementDescriptor) {
            Object beanInstance = descriptor.resolveCandidate(candidateName, requiredType, this);
            if (!(beanInstance instanceof NullBean)) {
                candidates.put(candidateName, beanInstance);
            }
        } else if (containsSingleton(candidateName) || (descriptor instanceof StreamDependencyDescriptor && ((StreamDependencyDescriptor) descriptor).isOrdered())) {
            Object beanInstance = descriptor.resolveCandidate(candidateName, requiredType, this);
            candidates.put(candidateName, (beanInstance instanceof NullBean ? null : beanInstance));
        } else {
            candidates.put(candidateName, getType(candidateName));
        }
    }

    @Nullable
    protected String determineAutowireCandidate(Map<String, Object> candidates, DependencyDescriptor descriptor) {
        Class<?> requiredType = descriptor.getDependencyType();
        String primaryCandidate = determinePrimaryCandidate(candidates, requiredType);
        if (primaryCandidate != null) {
            return primaryCandidate;
        }
        String priorityCandidate = determineHighestPriorityCandidate(candidates, requiredType);
        if (priorityCandidate != null) {
            return priorityCandidate;
        }
        // Fallback
        for (Map.Entry<String, Object> entry : candidates.entrySet()) {
            String candidateName = entry.getKey();
            Object beanInstance = entry.getValue();
            if ((beanInstance != null && this.resolvableDependencies.containsValue(beanInstance)) || matchesBeanName(candidateName, descriptor.getDependencyName())) {
                return candidateName;
            }
        }
        return null;
    }

    @Nullable
    protected String determinePrimaryCandidate(Map<String, Object> candidates, Class<?> requiredType) {
        String primaryBeanName = null;
        for (Map.Entry<String, Object> entry : candidates.entrySet()) {
            String candidateBeanName = entry.getKey();
            Object beanInstance = entry.getValue();
            if (isPrimary(candidateBeanName, beanInstance)) {
                if (primaryBeanName != null) {
                    boolean candidateLocal = containsBeanDefinition(candidateBeanName);
                    boolean primaryLocal = containsBeanDefinition(primaryBeanName);
                    if (candidateLocal && primaryLocal) {
                        throw new NoUniqueBeanDefinitionException(requiredType, candidates.size(), "more than one 'primary' bean found among candidates: " + candidates.keySet());
                    } else if (candidateLocal) {
                        primaryBeanName = candidateBeanName;
                    }
                } else {
                    primaryBeanName = candidateBeanName;
                }
            }
        }
        return primaryBeanName;
    }

    @Nullable
    protected String determineHighestPriorityCandidate(Map<String, Object> candidates, Class<?> requiredType) {
        String highestPriorityBeanName = null;
        Integer highestPriority = null;
        for (Map.Entry<String, Object> entry : candidates.entrySet()) {
            String candidateBeanName = entry.getKey();
            Object beanInstance = entry.getValue();
            if (beanInstance != null) {
                Integer candidatePriority = getPriority(beanInstance);
                if (candidatePriority != null) {
                    if (highestPriorityBeanName != null) {
                        if (candidatePriority.equals(highestPriority)) {
                            throw new NoUniqueBeanDefinitionException(requiredType, candidates.size(), "Multiple beans found with the same priority ('" + highestPriority + "') among candidates: " + candidates.keySet());
                        } else if (candidatePriority < highestPriority) {
                            highestPriorityBeanName = candidateBeanName;
                            highestPriority = candidatePriority;
                        }
                    } else {
                        highestPriorityBeanName = candidateBeanName;
                        highestPriority = candidatePriority;
                    }
                }
            }
        }
        return highestPriorityBeanName;
    }

    protected boolean isPrimary(String beanName, Object beanInstance) {
        String transformedBeanName = transformedBeanName(beanName);
        if (containsBeanDefinition(transformedBeanName)) {
            return getMergedLocalBeanDefinition(transformedBeanName).isPrimary();
        }
        BeanFactory parent = getParentBeanFactory();
        return (parent instanceof DefaultListableBeanFactory && ((DefaultListableBeanFactory) parent).isPrimary(transformedBeanName, beanInstance));
    }

    @Nullable
    protected Integer getPriority(Object beanInstance) {
        Comparator<Object> comparator = getDependencyComparator();
        if (comparator instanceof OrderComparator) {
            return ((OrderComparator) comparator).getPriority(beanInstance);
        }
        return null;
    }

    protected boolean matchesBeanName(String beanName, @Nullable String candidateName) {
        return (candidateName != null && (candidateName.equals(beanName) || ObjectUtils.containsElement(getAliases(beanName), candidateName)));
    }

    private boolean isSelfReference(@Nullable String beanName, @Nullable String candidateName) {
        return (beanName != null && candidateName != null && (beanName.equals(candidateName) || (containsBeanDefinition(candidateName) && beanName.equals(getMergedLocalBeanDefinition(candidateName).getFactoryBeanName()))));
    }

    private void raiseNoMatchingBeanFound(Class<?> type, ResolvableType resolvableType, DependencyDescriptor descriptor) throws BeansException {
        checkBeanNotOfRequiredType(type, descriptor);
        throw new NoSuchBeanDefinitionException(resolvableType, "expected at least 1 bean which qualifies as autowire candidate. " + "Dependency annotations: " + ObjectUtils.nullSafeToString(descriptor.getAnnotations()));
    }

    private void checkBeanNotOfRequiredType(Class<?> type, DependencyDescriptor descriptor) {
        for (String beanName : this.beanDefinitionNames) {
            RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
            Class<?> targetType = mbd.getTargetType();
            if (targetType != null && type.isAssignableFrom(targetType) && isAutowireCandidate(beanName, mbd, descriptor, getAutowireCandidateResolver())) {
                // Probably a proxy interfering with target type match -> throw meaningful exception.
                Object beanInstance = getSingleton(beanName, false);
                Class<?> beanType = (beanInstance != null && beanInstance.getClass() != NullBean.class ? beanInstance.getClass() : predictBeanType(beanName, mbd));
                if (beanType != null && !type.isAssignableFrom(beanType)) {
                    throw new BeanNotOfRequiredTypeException(beanName, type, beanType);
                }
            }
        }
        BeanFactory parent = getParentBeanFactory();
        if (parent instanceof DefaultListableBeanFactory) {
            ((DefaultListableBeanFactory) parent).checkBeanNotOfRequiredType(type, descriptor);
        }
    }

    private Optional<?> createOptionalDependency(DependencyDescriptor descriptor, @Nullable String beanName, final Object... args) {
        DependencyDescriptor descriptorToUse = new NestedDependencyDescriptor(descriptor) {
            @Override
            public boolean isRequired() {
                return false;
            }

            @Override
            public Object resolveCandidate(String beanName, Class<?> requiredType, BeanFactory beanFactory) {
                return (!ObjectUtils.isEmpty(args) ? beanFactory.getBean(beanName, args) : super.resolveCandidate(beanName, requiredType, beanFactory));
            }
        };
        Object result = doResolveDependency(descriptorToUse, beanName, null, null);
        return (result instanceof Optional ? (Optional<?>) result : Optional.ofNullable(result));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(ObjectUtils.identityToString(this));
        sb.append(": defining beans [");
        sb.append(StringUtils.collectionToCommaDelimitedString(this.beanDefinitionNames));
        sb.append("]; ");
        BeanFactory parent = getParentBeanFactory();
        if (parent == null) {
            sb.append("root of factory hierarchy");
        } else {
            sb.append("parent: ").append(ObjectUtils.identityToString(parent));
        }
        return sb.toString();
    }
    //---------------------------------------------------------------------
    // Serialization support
    //---------------------------------------------------------------------

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        throw new NotSerializableException("DefaultListableBeanFactory itself is not deserializable - " + "just a SerializedBeanFactoryReference is");
    }

    protected Object writeReplace() throws ObjectStreamException {
        if (this.serializationId != null) {
            return new SerializedBeanFactoryReference(this.serializationId);
        } else {
            throw new NotSerializableException("DefaultListableBeanFactory has no serialization id");
        }
    }

    private static class SerializedBeanFactoryReference implements Serializable {

        private final String id;

        public SerializedBeanFactoryReference(String id) {
            this.id = id;
        }

        private Object readResolve() {
            Reference<?> ref = serializableFactories.get(this.id);
            if (ref != null) {
                Object result = ref.get();
                if (result != null) {
                    return result;
                }
            }
            // Lenient fallback: dummy factory in case of original factory not found...
            DefaultListableBeanFactory dummyFactory = new DefaultListableBeanFactory();
            dummyFactory.serializationId = this.id;
            return dummyFactory;
        }

    }

    private static class NestedDependencyDescriptor extends DependencyDescriptor {

        public NestedDependencyDescriptor(DependencyDescriptor original) {
            super(original);
            increaseNestingLevel();
        }

    }

    private static class MultiElementDescriptor extends NestedDependencyDescriptor {

        public MultiElementDescriptor(DependencyDescriptor original) {
            super(original);
        }

    }

    private static class StreamDependencyDescriptor extends DependencyDescriptor {

        private final boolean ordered;

        public StreamDependencyDescriptor(DependencyDescriptor original, boolean ordered) {
            super(original);
            this.ordered = ordered;
        }

        public boolean isOrdered() {
            return this.ordered;
        }

    }

    private interface BeanObjectProvider<T> extends ObjectProvider<T>, Serializable {
    }

    private class DependencyObjectProvider implements BeanObjectProvider<Object> {

        private final DependencyDescriptor descriptor;

        private final boolean optional;

        @Nullable
        private final String beanName;

        public DependencyObjectProvider(DependencyDescriptor descriptor, @Nullable String beanName) {
            this.descriptor = new NestedDependencyDescriptor(descriptor);
            this.optional = (this.descriptor.getDependencyType() == Optional.class);
            this.beanName = beanName;
        }

        @Override
        public Object getObject() throws BeansException {
            if (this.optional) {
                return createOptionalDependency(this.descriptor, this.beanName);
            } else {
                Object result = doResolveDependency(this.descriptor, this.beanName, null, null);
                if (result == null) {
                    throw new NoSuchBeanDefinitionException(this.descriptor.getResolvableType());
                }
                return result;
            }
        }

        @Override
        public Object getObject(final Object... args) throws BeansException {
            if (this.optional) {
                return createOptionalDependency(this.descriptor, this.beanName, args);
            } else {
                DependencyDescriptor descriptorToUse = new DependencyDescriptor(this.descriptor) {
                    @Override
                    public Object resolveCandidate(String beanName, Class<?> requiredType, BeanFactory beanFactory) {
                        return beanFactory.getBean(beanName, args);
                    }
                };
                Object result = doResolveDependency(descriptorToUse, this.beanName, null, null);
                if (result == null) {
                    throw new NoSuchBeanDefinitionException(this.descriptor.getResolvableType());
                }
                return result;
            }
        }

        @Override
        @Nullable
        public Object getIfAvailable() throws BeansException {
            if (this.optional) {
                return createOptionalDependency(this.descriptor, this.beanName);
            } else {
                DependencyDescriptor descriptorToUse = new DependencyDescriptor(this.descriptor) {
                    @Override
                    public boolean isRequired() {
                        return false;
                    }
                };
                return doResolveDependency(descriptorToUse, this.beanName, null, null);
            }
        }

        @Override
        @Nullable
        public Object getIfUnique() throws BeansException {
            DependencyDescriptor descriptorToUse = new DependencyDescriptor(this.descriptor) {
                @Override
                public boolean isRequired() {
                    return false;
                }

                @Override
                @Nullable
                public Object resolveNotUnique(ResolvableType type, Map<String, Object> matchingBeans) {
                    return null;
                }
            };
            if (this.optional) {
                return createOptionalDependency(descriptorToUse, this.beanName);
            } else {
                return doResolveDependency(descriptorToUse, this.beanName, null, null);
            }
        }

        @Nullable
        protected Object getValue() throws BeansException {
            if (this.optional) {
                return createOptionalDependency(this.descriptor, this.beanName);
            } else {
                return doResolveDependency(this.descriptor, this.beanName, null, null);
            }
        }

        @Override
        public Stream<Object> stream() {
            return resolveStream(false);
        }

        @Override
        public Stream<Object> orderedStream() {
            return resolveStream(true);
        }

        @SuppressWarnings("unchecked")
        private Stream<Object> resolveStream(boolean ordered) {
            DependencyDescriptor descriptorToUse = new StreamDependencyDescriptor(this.descriptor, ordered);
            Object result = doResolveDependency(descriptorToUse, this.beanName, null, null);
            return (result instanceof Stream ? (Stream<Object>) result : Stream.of(result));
        }

    }

    private class Jsr330Factory implements Serializable {

        public Object createDependencyProvider(DependencyDescriptor descriptor, @Nullable String beanName) {
            return new Jsr330Provider(descriptor, beanName);
        }

        private class Jsr330Provider extends DependencyObjectProvider implements Provider<Object> {

            public Jsr330Provider(DependencyDescriptor descriptor, @Nullable String beanName) {
                super(descriptor, beanName);
            }

            @Override
            @Nullable
            public Object get() throws BeansException {
                return getValue();
            }

        }

    }

    private class FactoryAwareOrderSourceProvider implements OrderComparator.OrderSourceProvider {

        private final Map<Object, String> instancesToBeanNames;

        public FactoryAwareOrderSourceProvider(Map<Object, String> instancesToBeanNames) {
            this.instancesToBeanNames = instancesToBeanNames;
        }

        @Override
        @Nullable
        public Object getOrderSource(Object obj) {
            String beanName = this.instancesToBeanNames.get(obj);
            if (beanName == null || !containsBeanDefinition(beanName)) {
                return null;
            }
            RootBeanDefinition beanDefinition = getMergedLocalBeanDefinition(beanName);
            List<Object> sources = new ArrayList<>(2);
            Method factoryMethod = beanDefinition.getResolvedFactoryMethod();
            if (factoryMethod != null) {
                sources.add(factoryMethod);
            }
            Class<?> targetType = beanDefinition.getTargetType();
            if (targetType != null && targetType != obj.getClass()) {
                sources.add(targetType);
            }
            return sources.toArray();
        }

    }

}
