package org.springframework.beans.factory.support;

import org.springframework.beans.*;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.config.*;
import org.springframework.core.DecoratingClassLoader;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.*;

import java.beans.PropertyEditor;
import java.security.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class AbstractBeanFactory extends FactoryBeanRegistrySupport implements ConfigurableBeanFactory {

    @Nullable
    private BeanFactory parentBeanFactory;

    @Nullable
    private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

    @Nullable
    private ClassLoader tempClassLoader;

    private boolean cacheBeanMetadata = true;

    @Nullable
    private BeanExpressionResolver beanExpressionResolver;

    @Nullable
    private ConversionService conversionService;

    private final Set<PropertyEditorRegistrar> propertyEditorRegistrars = new LinkedHashSet<>(4);

    private final Map<Class<?>, Class<? extends PropertyEditor>> customEditors = new HashMap<>(4);

    @Nullable
    private TypeConverter typeConverter;

    private final List<StringValueResolver> embeddedValueResolvers = new CopyOnWriteArrayList<>();

    private final List<BeanPostProcessor> beanPostProcessors = new CopyOnWriteArrayList<>();

    private volatile boolean hasInstantiationAwareBeanPostProcessors;

    private volatile boolean hasDestructionAwareBeanPostProcessors;

    private final Map<String, Scope> scopes = new LinkedHashMap<>(8);

    @Nullable
    private SecurityContextProvider securityContextProvider;

    private final Map<String, RootBeanDefinition> mergedBeanDefinitions = new ConcurrentHashMap<>(256);

    private final Set<String> alreadyCreated = Collections.newSetFromMap(new ConcurrentHashMap<>(256));

    private final ThreadLocal<Object> prototypesCurrentlyInCreation = new NamedThreadLocal<>("Prototype beans currently in creation");

    public AbstractBeanFactory() {
    }

    public AbstractBeanFactory(@Nullable BeanFactory parentBeanFactory) {
        this.parentBeanFactory = parentBeanFactory;
    }
    //---------------------------------------------------------------------
    // Implementation of BeanFactory interface
    //---------------------------------------------------------------------

    @Override
    public Object getBean(String name) throws BeansException {
        return doGetBean(name, null, null, false);
    }

    @Override
    public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
        return doGetBean(name, requiredType, null, false);
    }

    @Override
    public Object getBean(String name, Object... args) throws BeansException {
        return doGetBean(name, null, args, false);
    }

    public <T> T getBean(String name, @Nullable Class<T> requiredType, @Nullable Object... args) throws BeansException {
        return doGetBean(name, requiredType, args, false);
    }

    /**
     * 单例：判断是否已经初始化并添加到缓存
     * 原型：重新初始化
     */
    @SuppressWarnings("unchecked")
    protected <T> T doGetBean(final String name, @Nullable final Class<T> requiredType, @Nullable final Object[] args, boolean typeCheckOnly) throws BeansException {
        /*
         * 别名转换，返回原始beanName：
         *      &开头：FactoryBean，返回去掉前缀的原名
         *      别名或原名：返回原名
         */
        final String beanName = transformedBeanName(name);
        // 返回值
        Object bean;
        /*
         * 先按顺序查缓存：
         *      this.singletonObjects：存放已经完成初始化的单例（普通单例Bean和FactoryBean单例Bean，FactoryBean生成的单例Bean在factoryBeanObjectCache）
         *      this.earlySingletonObjects：存放已实例化但还没有初始化注入依赖的早期引用，完成初始化后删除
         *      this.singletonFactories：存放生成早期引用的ObjectFactory，用完就删
         */
        Object sharedInstance = getSingleton(beanName);
        // 已存在
        if (sharedInstance != null && args == null) {
            if (logger.isTraceEnabled()) {
                // singletonsCurrentlyInCreation包含beanName
                if (isSingletonCurrentlyInCreation(beanName)) {
                    logger.trace("Returning eagerly cached instance of singleton bean '" + beanName + "' that is not fully initialized yet - a consequence of a circular reference");
                } else {
                    logger.trace("Returning cached instance of singleton bean '" + beanName + "'");
                }
            }
            /*
             * 以&开头，如果beanInstance不是FactoryBean类型，抛异常
             * 以&开头，如果beanInstance是FactoryBean类型，直接返回beanInstance
             * 不以&开头，如果beanInstance不是FactoryBean类型，直接返回beanInstance
             * 不以&开头，如果beanInstance是FactoryBean类型
             *      单例：先从factoryBeanObjectCache获取，为空则调用FactoryBean#getObject创建，并放入factoryBeanObjectCache
             *      原型：不加入factoryBeanObjectCache，每次都重新创建
             */
            bean = getObjectForBeanInstance(sharedInstance, name, beanName, null);
        }
        // 第一次创建，可能是原型或单例
        else {
            // 获取Bean之前如果这个原型Bean正在被创建则直接抛出异常，原型Bean在创建之前会标记这个beanName正在被创建，创建结束之后会删除标记
            if (isPrototypeCurrentlyInCreation(beanName)) {
                // 不支持原型的循环依赖，不论是构造器参数循环依赖，还是setXxx方法循环依赖
                throw new BeanCurrentlyInCreationException(beanName);
            }
            // 父BeanFactory
            BeanFactory parentBeanFactory = getParentBeanFactory();
            // 当前容器beanDefinitionMap不存在这个BeanDefinition，查找父容器
            if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
                // Not found -> check parent.
                String nameToLookup = originalBeanName(name);
                if (parentBeanFactory instanceof AbstractBeanFactory) {
                    return ((AbstractBeanFactory) parentBeanFactory).doGetBean(nameToLookup, requiredType, args, typeCheckOnly);
                } else if (args != null) {
                    // 返回父容器的查询结果
                    return (T) parentBeanFactory.getBean(nameToLookup, args);
                } else if (requiredType != null) {
                    // No args -> delegate to standard getBean method.
                    return parentBeanFactory.getBean(nameToLookup, requiredType);
                } else {
                    return (T) parentBeanFactory.getBean(nameToLookup);
                }
            }
            // typeCheckOnly == false
            if (!typeCheckOnly) {
                // 将beanName放入alreadyCreated的Set集合
                // 将beanName从mergedBeanDefinitions的Map集合移除
                markBeanAsCreated(beanName);
            }
            // 准备创建Bean，对于Singleton，还没创建过，对于Prototype，创建一个新的
            try {
                // 从mergedBeanDefinitions获取
                final RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
                checkMergedBeanDefinition(mbd, beanName, args);
                // 先初始化depends-on的所有Bean
                String[] dependsOn = mbd.getDependsOn();
                if (dependsOn != null) {
                    for (String dep : dependsOn) {
                        // 检查是不是有循环依赖
                        if (isDependent(beanName, dep)) {
                            throw new BeanCreationException(mbd.getResourceDescription(), beanName, "Circular depends-on relationship between '" + beanName + "' and '" + dep + "'");
                        }
                        // 注册依赖关系
                        registerDependentBean(dep, beanName);
                        try {
                            // 先初始化被依赖项
                            getBean(dep);
                        } catch (NoSuchBeanDefinitionException ex) {
                            throw new BeanCreationException(mbd.getResourceDescription(), beanName, "'" + beanName + "' depends on missing bean '" + dep + "'", ex);
                        }
                    }
                }
                // 如果是Singleton，创建Singleton实例
                if (mbd.isSingleton()) {
                    /*
                     * 获取Bean，生成一个匿名ObjectFactory对象传入，封装了创建方法，通过调用getObject来调用createBean创建：
                     *      再次尝试从singletonObjects获取，没获取到
                     *      创建前：尝试将beanName添加到singletonsCurrentlyInCreation，如果已存在，抛异常
                     *      调用createBean创建Bean
                     *      创建后：尝试将beanName从singletonsCurrentlyInCreation移除，如果不存在，抛异常
                     *      添加创建的实例到singletonObjects
                     *      从this.singletonFactories移除beanName键值
                     *      从this.earlySingletonObjects移除beanName键值
                     *      添加beanName到this.registeredSingletons
                     */
                    sharedInstance = getSingleton(beanName, () -> {
                        try {
                            // 执行创建Bean
                            return createBean(beanName, mbd, args);
                        } catch (BeansException ex) {
                            // Explicitly remove instance from singleton cache: It might have been put there
                            // eagerly by the creation process, to allow for circular reference resolution.
                            // Also remove any beans that received a temporary reference to the bean.
                            destroySingleton(beanName);
                            throw ex;
                        }
                    });
                    // 参考上面
                    bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
                }
                // 如果是Prototype，创建Prototype实例
                else if (mbd.isPrototype()) {
                    Object prototypeInstance = null;
                    try {
                        // 创建原型之前添加标记，添加beanName到prototypesCurrentlyInCreation
                        beforePrototypeCreation(beanName);
                        // 执行创建Bean
                        prototypeInstance = createBean(beanName, mbd, args);
                    } finally {
                        // 创建原型之后删除标记，将beanName从prototypesCurrentlyInCreation移除
                        afterPrototypeCreation(beanName);
                    }
                    bean = getObjectForBeanInstance(prototypeInstance, name, beanName, mbd);
                }
                // 如果不是Singleton或Prototype
                else {
                    String scopeName = mbd.getScope();
                    final Scope scope = this.scopes.get(scopeName);
                    if (scope == null) {
                        throw new IllegalStateException("No Scope registered for scope name '" + scopeName + "'");
                    }
                    try {
                        Object scopedInstance = scope.get(beanName, () -> {
                            beforePrototypeCreation(beanName);
                            try {
                                // 执行创建Bean
                                return createBean(beanName, mbd, args);
                            } finally {
                                afterPrototypeCreation(beanName);
                            }
                        });
                        bean = getObjectForBeanInstance(scopedInstance, name, beanName, mbd);
                    } catch (IllegalStateException ex) {
                        throw new BeanCreationException(beanName, "Scope '" + scopeName + "' is not active for the current thread; consider " + "defining a scoped proxy for this bean if you intend to refer to it from a singleton", ex);
                    }
                }
            } catch (BeansException ex) {
                cleanupAfterBeanCreationFailure(beanName);
                throw ex;
            }
        }
        // 类型有要求 && 实例不是要求的类型
        if (requiredType != null && !requiredType.isInstance(bean)) {
            try {
                // 转化类型
                T convertedBean = getTypeConverter().convertIfNecessary(bean, requiredType);
                if (convertedBean == null) {
                    throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
                }
                // 返回转化后的类型
                return convertedBean;
            } catch (TypeMismatchException ex) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Failed to convert bean '" + name + "' to required type '" + ClassUtils.getQualifiedName(requiredType) + "'", ex);
                }
                throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
            }
        }
        // 直接返回
        return (T) bean;
    }

    /**
     * 检查singletonObjects
     * 检查beanDefinitionMap
     */
    @Override
    public boolean containsBean(String name) {
        String beanName = transformedBeanName(name);
        if (containsSingleton(beanName) || containsBeanDefinition(beanName)) {
            return (!BeanFactoryUtils.isFactoryDereference(name) || isFactoryBean(name));
        }
        // Not found -> check parent.
        BeanFactory parentBeanFactory = getParentBeanFactory();
        return (parentBeanFactory != null && parentBeanFactory.containsBean(originalBeanName(name)));
    }

    @Override
    public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
        String beanName = transformedBeanName(name);
        Object beanInstance = getSingleton(beanName, false);
        if (beanInstance != null) {
            if (beanInstance instanceof FactoryBean) {
                return (BeanFactoryUtils.isFactoryDereference(name) || ((FactoryBean<?>) beanInstance).isSingleton());
            } else {
                return !BeanFactoryUtils.isFactoryDereference(name);
            }
        }
        // No singleton instance found -> check bean definition.
        BeanFactory parentBeanFactory = getParentBeanFactory();
        if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
            // No bean definition found in this factory -> delegate to parent.
            return parentBeanFactory.isSingleton(originalBeanName(name));
        }
        RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
        // In case of FactoryBean, return singleton status of created object if not a dereference.
        if (mbd.isSingleton()) {
            if (isFactoryBean(beanName, mbd)) {
                if (BeanFactoryUtils.isFactoryDereference(name)) {
                    return true;
                }
                FactoryBean<?> factoryBean = (FactoryBean<?>) getBean(FACTORY_BEAN_PREFIX + beanName);
                return factoryBean.isSingleton();
            } else {
                return !BeanFactoryUtils.isFactoryDereference(name);
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
        String beanName = transformedBeanName(name);
        BeanFactory parentBeanFactory = getParentBeanFactory();
        if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
            // No bean definition found in this factory -> delegate to parent.
            return parentBeanFactory.isPrototype(originalBeanName(name));
        }
        RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
        if (mbd.isPrototype()) {
            // In case of FactoryBean, return singleton status of created object if not a dereference.
            return (!BeanFactoryUtils.isFactoryDereference(name) || isFactoryBean(beanName, mbd));
        }
        // Singleton or scoped - not a prototype.
        // However, FactoryBean may still produce a prototype object...
        if (BeanFactoryUtils.isFactoryDereference(name)) {
            return false;
        }
        if (isFactoryBean(beanName, mbd)) {
            final FactoryBean<?> fb = (FactoryBean<?>) getBean(FACTORY_BEAN_PREFIX + beanName);
            if (System.getSecurityManager() != null) {
                return AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> ((fb instanceof SmartFactoryBean && ((SmartFactoryBean<?>) fb).isPrototype()) || !fb.isSingleton()), getAccessControlContext());
            } else {
                return ((fb instanceof SmartFactoryBean && ((SmartFactoryBean<?>) fb).isPrototype()) || !fb.isSingleton());
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
        // 普通的bean则beanName不变，如果是FactoryBean，则去掉前缀'&'
        String beanName = transformedBeanName(name);
        // Check manually registered singletons.
        Object beanInstance = getSingleton(beanName, false);
        if (beanInstance != null && beanInstance.getClass() != NullBean.class) {
            if (beanInstance instanceof FactoryBean) {
                if (!BeanFactoryUtils.isFactoryDereference(name)) {
                    Class<?> type = getTypeForFactoryBean((FactoryBean<?>) beanInstance);
                    return (type != null && typeToMatch.isAssignableFrom(type));
                } else {
                    return typeToMatch.isInstance(beanInstance);
                }
            } else if (!BeanFactoryUtils.isFactoryDereference(name)) {
                if (typeToMatch.isInstance(beanInstance)) {
                    // Direct match for exposed instance?
                    return true;
                } else if (typeToMatch.hasGenerics() && containsBeanDefinition(beanName)) {
                    // Generics potentially only match on the target class, not on the proxy...
                    RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
                    Class<?> targetType = mbd.getTargetType();
                    if (targetType != null && targetType != ClassUtils.getUserClass(beanInstance)) {
                        // Check raw class match as well, making sure it's exposed on the proxy.
                        Class<?> classToMatch = typeToMatch.resolve();
                        if (classToMatch != null && !classToMatch.isInstance(beanInstance)) {
                            return false;
                        }
                        if (typeToMatch.isAssignableFrom(targetType)) {
                            return true;
                        }
                    }
                    ResolvableType resolvableType = mbd.targetType;
                    if (resolvableType == null) {
                        resolvableType = mbd.factoryMethodReturnType;
                    }
                    return (resolvableType != null && typeToMatch.isAssignableFrom(resolvableType));
                }
            }
            return false;
        }
        // singletonObjects包含 && beanDefinitionMap中不存在
        else if (containsSingleton(beanName) && !containsBeanDefinition(beanName)) {
            // null instance registered
            // 其他线程正在并发移除Bean
            return false;
        }
        // No singleton instance found -> check bean definition.
        BeanFactory parentBeanFactory = getParentBeanFactory();
        if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
            // No bean definition found in this factory -> delegate to parent.
            return parentBeanFactory.isTypeMatch(originalBeanName(name), typeToMatch);
        }
        // Retrieve corresponding bean definition.
        RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
        Class<?> classToMatch = typeToMatch.resolve();
        if (classToMatch == null) {
            classToMatch = FactoryBean.class;
        }
        Class<?>[] typesToMatch = (FactoryBean.class == classToMatch ? new Class<?>[]{classToMatch} : new Class<?>[]{FactoryBean.class, classToMatch});
        // Check decorated bean definition, if any: We assume it'll be easier
        // to determine the decorated bean's type than the proxy's type.
        BeanDefinitionHolder dbd = mbd.getDecoratedDefinition();
        if (dbd != null && !BeanFactoryUtils.isFactoryDereference(name)) {
            RootBeanDefinition tbd = getMergedBeanDefinition(dbd.getBeanName(), dbd.getBeanDefinition(), mbd);
            Class<?> targetClass = predictBeanType(dbd.getBeanName(), tbd, typesToMatch);
            if (targetClass != null && !FactoryBean.class.isAssignableFrom(targetClass)) {
                return typeToMatch.isAssignableFrom(targetClass);
            }
        }
        Class<?> beanType = predictBeanType(beanName, mbd, typesToMatch);
        if (beanType == null) {
            return false;
        }
        // Check bean class whether we're dealing with a FactoryBean.
        if (FactoryBean.class.isAssignableFrom(beanType)) {
            if (!BeanFactoryUtils.isFactoryDereference(name) && beanInstance == null) {
                // If it's a FactoryBean, we want to look at what it creates, not the factory class.
                beanType = getTypeForFactoryBean(beanName, mbd);
                if (beanType == null) {
                    return false;
                }
            }
        } else if (BeanFactoryUtils.isFactoryDereference(name)) {
            // Special case: A SmartInstantiationAwareBeanPostProcessor returned a non-FactoryBean
            // type but we nevertheless are being asked to dereference a FactoryBean...
            // Let's check the original bean class and proceed with it if it is a FactoryBean.
            beanType = predictBeanType(beanName, mbd, FactoryBean.class);
            if (beanType == null || !FactoryBean.class.isAssignableFrom(beanType)) {
                return false;
            }
        }
        ResolvableType resolvableType = mbd.targetType;
        if (resolvableType == null) {
            resolvableType = mbd.factoryMethodReturnType;
        }
        if (resolvableType != null && resolvableType.resolve() == beanType) {
            return typeToMatch.isAssignableFrom(resolvableType);
        }
        return typeToMatch.isAssignableFrom(beanType);
    }

    @Override
    public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
        return isTypeMatch(name, ResolvableType.forRawClass(typeToMatch));
    }

    @Override
    @Nullable
    public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
        String beanName = transformedBeanName(name);
        // Check manually registered singletons.
        Object beanInstance = getSingleton(beanName, false);
        if (beanInstance != null && beanInstance.getClass() != NullBean.class) {
            if (beanInstance instanceof FactoryBean && !BeanFactoryUtils.isFactoryDereference(name)) {
                return getTypeForFactoryBean((FactoryBean<?>) beanInstance);
            } else {
                return beanInstance.getClass();
            }
        }
        // No singleton instance found -> check bean definition.
        BeanFactory parentBeanFactory = getParentBeanFactory();
        if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
            // No bean definition found in this factory -> delegate to parent.
            return parentBeanFactory.getType(originalBeanName(name));
        }
        RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
        // Check decorated bean definition, if any: We assume it'll be easier
        // to determine the decorated bean's type than the proxy's type.
        BeanDefinitionHolder dbd = mbd.getDecoratedDefinition();
        if (dbd != null && !BeanFactoryUtils.isFactoryDereference(name)) {
            RootBeanDefinition tbd = getMergedBeanDefinition(dbd.getBeanName(), dbd.getBeanDefinition(), mbd);
            Class<?> targetClass = predictBeanType(dbd.getBeanName(), tbd);
            if (targetClass != null && !FactoryBean.class.isAssignableFrom(targetClass)) {
                return targetClass;
            }
        }
        Class<?> beanClass = predictBeanType(beanName, mbd);
        // Check bean class whether we're dealing with a FactoryBean.
        if (beanClass != null && FactoryBean.class.isAssignableFrom(beanClass)) {
            if (!BeanFactoryUtils.isFactoryDereference(name)) {
                // If it's a FactoryBean, we want to look at what it creates, not at the factory class.
                return getTypeForFactoryBean(beanName, mbd);
            } else {
                return beanClass;
            }
        } else {
            return (!BeanFactoryUtils.isFactoryDereference(name) ? beanClass : null);
        }
    }

    @Override
    public String[] getAliases(String name) {
        String beanName = transformedBeanName(name);
        List<String> aliases = new ArrayList<>();
        boolean factoryPrefix = name.startsWith(FACTORY_BEAN_PREFIX);
        String fullBeanName = beanName;
        if (factoryPrefix) {
            fullBeanName = FACTORY_BEAN_PREFIX + beanName;
        }
        if (!fullBeanName.equals(name)) {
            aliases.add(fullBeanName);
        }
        String[] retrievedAliases = super.getAliases(beanName);
        for (String retrievedAlias : retrievedAliases) {
            String alias = (factoryPrefix ? FACTORY_BEAN_PREFIX : "") + retrievedAlias;
            if (!alias.equals(name)) {
                aliases.add(alias);
            }
        }
        if (!containsSingleton(beanName) && !containsBeanDefinition(beanName)) {
            BeanFactory parentBeanFactory = getParentBeanFactory();
            if (parentBeanFactory != null) {
                aliases.addAll(Arrays.asList(parentBeanFactory.getAliases(fullBeanName)));
            }
        }
        return StringUtils.toStringArray(aliases);
    }
    //---------------------------------------------------------------------
    // Implementation of HierarchicalBeanFactory interface
    //---------------------------------------------------------------------

    @Override
    @Nullable
    public BeanFactory getParentBeanFactory() {
        return this.parentBeanFactory;
    }

    @Override
    public boolean containsLocalBean(String name) {
        String beanName = transformedBeanName(name);
        return ((containsSingleton(beanName) || containsBeanDefinition(beanName)) && (!BeanFactoryUtils.isFactoryDereference(name) || isFactoryBean(beanName)));
    }
    //---------------------------------------------------------------------
    // Implementation of ConfigurableBeanFactory interface
    //---------------------------------------------------------------------

    @Override
    public void setParentBeanFactory(@Nullable BeanFactory parentBeanFactory) {
        if (this.parentBeanFactory != null && this.parentBeanFactory != parentBeanFactory) {
            throw new IllegalStateException("Already associated with parent BeanFactory: " + this.parentBeanFactory);
        }
        this.parentBeanFactory = parentBeanFactory;
    }

    @Override
    public void setBeanClassLoader(@Nullable ClassLoader beanClassLoader) {
        this.beanClassLoader = (beanClassLoader != null ? beanClassLoader : ClassUtils.getDefaultClassLoader());
    }

    @Override
    @Nullable
    public ClassLoader getBeanClassLoader() {
        return this.beanClassLoader;
    }

    @Override
    public void setTempClassLoader(@Nullable ClassLoader tempClassLoader) {
        this.tempClassLoader = tempClassLoader;
    }

    @Override
    @Nullable
    public ClassLoader getTempClassLoader() {
        return this.tempClassLoader;
    }

    @Override
    public void setCacheBeanMetadata(boolean cacheBeanMetadata) {
        this.cacheBeanMetadata = cacheBeanMetadata;
    }

    @Override
    public boolean isCacheBeanMetadata() {
        return this.cacheBeanMetadata;
    }

    @Override
    public void setBeanExpressionResolver(@Nullable BeanExpressionResolver resolver) {
        this.beanExpressionResolver = resolver;
    }

    @Override
    @Nullable
    public BeanExpressionResolver getBeanExpressionResolver() {
        return this.beanExpressionResolver;
    }

    @Override
    public void setConversionService(@Nullable ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    @Nullable
    public ConversionService getConversionService() {
        return this.conversionService;
    }

    @Override
    public void addPropertyEditorRegistrar(PropertyEditorRegistrar registrar) {
        Assert.notNull(registrar, "PropertyEditorRegistrar must not be null");
        this.propertyEditorRegistrars.add(registrar);
    }

    public Set<PropertyEditorRegistrar> getPropertyEditorRegistrars() {
        return this.propertyEditorRegistrars;
    }

    @Override
    public void registerCustomEditor(Class<?> requiredType, Class<? extends PropertyEditor> propertyEditorClass) {
        Assert.notNull(requiredType, "Required type must not be null");
        Assert.notNull(propertyEditorClass, "PropertyEditor class must not be null");
        this.customEditors.put(requiredType, propertyEditorClass);
    }

    @Override
    public void copyRegisteredEditorsTo(PropertyEditorRegistry registry) {
        registerCustomEditors(registry);
    }

    public Map<Class<?>, Class<? extends PropertyEditor>> getCustomEditors() {
        return this.customEditors;
    }

    @Override
    public void setTypeConverter(TypeConverter typeConverter) {
        this.typeConverter = typeConverter;
    }

    @Nullable
    protected TypeConverter getCustomTypeConverter() {
        return this.typeConverter;
    }

    @Override
    public TypeConverter getTypeConverter() {
        TypeConverter customConverter = getCustomTypeConverter();
        if (customConverter != null) {
            return customConverter;
        } else {
            // Build default TypeConverter, registering custom editors.
            SimpleTypeConverter typeConverter = new SimpleTypeConverter();
            typeConverter.setConversionService(getConversionService());
            registerCustomEditors(typeConverter);
            return typeConverter;
        }
    }

    @Override
    public void addEmbeddedValueResolver(StringValueResolver valueResolver) {
        Assert.notNull(valueResolver, "StringValueResolver must not be null");
        this.embeddedValueResolvers.add(valueResolver);
    }

    @Override
    public boolean hasEmbeddedValueResolver() {
        return !this.embeddedValueResolvers.isEmpty();
    }

    @Override
    @Nullable
    public String resolveEmbeddedValue(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String result = value;
        for (StringValueResolver resolver : this.embeddedValueResolvers) {
            result = resolver.resolveStringValue(result);
            if (result == null) {
                return null;
            }
        }
        return result;
    }

    @Override
    public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
        Assert.notNull(beanPostProcessor, "BeanPostProcessor must not be null");
        // Remove from old position, if any
        this.beanPostProcessors.remove(beanPostProcessor);
        // Track whether it is instantiation/destruction aware
        if (beanPostProcessor instanceof InstantiationAwareBeanPostProcessor) {
            this.hasInstantiationAwareBeanPostProcessors = true;
        }
        if (beanPostProcessor instanceof DestructionAwareBeanPostProcessor) {
            this.hasDestructionAwareBeanPostProcessors = true;
        }
        // Add to end of list
        this.beanPostProcessors.add(beanPostProcessor);
    }

    @Override
    public int getBeanPostProcessorCount() {
        return this.beanPostProcessors.size();
    }

    public List<BeanPostProcessor> getBeanPostProcessors() {
        return this.beanPostProcessors;
    }

    protected boolean hasInstantiationAwareBeanPostProcessors() {
        return this.hasInstantiationAwareBeanPostProcessors;
    }

    protected boolean hasDestructionAwareBeanPostProcessors() {
        return this.hasDestructionAwareBeanPostProcessors;
    }

    @Override
    public void registerScope(String scopeName, Scope scope) {
        Assert.notNull(scopeName, "Scope identifier must not be null");
        Assert.notNull(scope, "Scope must not be null");
        if (SCOPE_SINGLETON.equals(scopeName) || SCOPE_PROTOTYPE.equals(scopeName)) {
            throw new IllegalArgumentException("Cannot replace existing scopes 'singleton' and 'prototype'");
        }
        Scope previous = this.scopes.put(scopeName, scope);
        if (previous != null && previous != scope) {
            if (logger.isDebugEnabled()) {
                logger.debug("Replacing scope '" + scopeName + "' from [" + previous + "] to [" + scope + "]");
            }
        } else {
            if (logger.isTraceEnabled()) {
                logger.trace("Registering scope '" + scopeName + "' with implementation [" + scope + "]");
            }
        }
    }

    @Override
    public String[] getRegisteredScopeNames() {
        return StringUtils.toStringArray(this.scopes.keySet());
    }

    @Override
    @Nullable
    public Scope getRegisteredScope(String scopeName) {
        Assert.notNull(scopeName, "Scope identifier must not be null");
        return this.scopes.get(scopeName);
    }

    public void setSecurityContextProvider(SecurityContextProvider securityProvider) {
        this.securityContextProvider = securityProvider;
    }

    @Override
    public AccessControlContext getAccessControlContext() {
        return (this.securityContextProvider != null ? this.securityContextProvider.getAccessControlContext() : AccessController.getContext());
    }

    @Override
    public void copyConfigurationFrom(ConfigurableBeanFactory otherFactory) {
        Assert.notNull(otherFactory, "BeanFactory must not be null");
        setBeanClassLoader(otherFactory.getBeanClassLoader());
        setCacheBeanMetadata(otherFactory.isCacheBeanMetadata());
        setBeanExpressionResolver(otherFactory.getBeanExpressionResolver());
        setConversionService(otherFactory.getConversionService());
        if (otherFactory instanceof AbstractBeanFactory) {
            AbstractBeanFactory otherAbstractFactory = (AbstractBeanFactory) otherFactory;
            this.propertyEditorRegistrars.addAll(otherAbstractFactory.propertyEditorRegistrars);
            this.customEditors.putAll(otherAbstractFactory.customEditors);
            this.typeConverter = otherAbstractFactory.typeConverter;
            this.beanPostProcessors.addAll(otherAbstractFactory.beanPostProcessors);
            this.hasInstantiationAwareBeanPostProcessors = this.hasInstantiationAwareBeanPostProcessors || otherAbstractFactory.hasInstantiationAwareBeanPostProcessors;
            this.hasDestructionAwareBeanPostProcessors = this.hasDestructionAwareBeanPostProcessors || otherAbstractFactory.hasDestructionAwareBeanPostProcessors;
            this.scopes.putAll(otherAbstractFactory.scopes);
            this.securityContextProvider = otherAbstractFactory.securityContextProvider;
        } else {
            setTypeConverter(otherFactory.getTypeConverter());
            String[] otherScopeNames = otherFactory.getRegisteredScopeNames();
            for (String scopeName : otherScopeNames) {
                this.scopes.put(scopeName, otherFactory.getRegisteredScope(scopeName));
            }
        }
    }

    @Override
    public BeanDefinition getMergedBeanDefinition(String name) throws BeansException {
        String beanName = transformedBeanName(name);
        // Efficiently check whether bean definition exists in this factory.
        if (!containsBeanDefinition(beanName) && getParentBeanFactory() instanceof ConfigurableBeanFactory) {
            return ((ConfigurableBeanFactory) getParentBeanFactory()).getMergedBeanDefinition(beanName);
        }
        // Resolve merged bean definition locally.
        return getMergedLocalBeanDefinition(beanName);
    }

    @Override
    public boolean isFactoryBean(String name) throws NoSuchBeanDefinitionException {
        // 原始beanName
        String beanName = transformedBeanName(name);
        // 先查单例缓存
        Object beanInstance = getSingleton(beanName, false);
        if (beanInstance != null) {
            //
            return (beanInstance instanceof FactoryBean);
        }
        // 没有就查BeanDefinition
        if (!containsBeanDefinition(beanName) && getParentBeanFactory() instanceof ConfigurableBeanFactory) {
            // 自身没有就从父级查
            return ((ConfigurableBeanFactory) getParentBeanFactory()).isFactoryBean(name);
        }
        // 从当前BeanFactory判断
        return isFactoryBean(beanName, getMergedLocalBeanDefinition(beanName));
    }

    @Override
    public boolean isActuallyInCreation(String beanName) {
        return (isSingletonCurrentlyInCreation(beanName) || isPrototypeCurrentlyInCreation(beanName));
    }

    protected boolean isPrototypeCurrentlyInCreation(String beanName) {
        Object curVal = this.prototypesCurrentlyInCreation.get();
        return (curVal != null && (curVal.equals(beanName) || (curVal instanceof Set && ((Set<?>) curVal).contains(beanName))));
    }

    @SuppressWarnings("unchecked")
    protected void beforePrototypeCreation(String beanName) {
        Object curVal = this.prototypesCurrentlyInCreation.get();
        if (curVal == null) {
            this.prototypesCurrentlyInCreation.set(beanName);
        } else if (curVal instanceof String) {
            Set<String> beanNameSet = new HashSet<>(2);
            beanNameSet.add((String) curVal);
            beanNameSet.add(beanName);
            this.prototypesCurrentlyInCreation.set(beanNameSet);
        } else {
            Set<String> beanNameSet = (Set<String>) curVal;
            beanNameSet.add(beanName);
        }
    }

    @SuppressWarnings("unchecked")
    protected void afterPrototypeCreation(String beanName) {
        Object curVal = this.prototypesCurrentlyInCreation.get();
        if (curVal instanceof String) {
            this.prototypesCurrentlyInCreation.remove();
        } else if (curVal instanceof Set) {
            Set<String> beanNameSet = (Set<String>) curVal;
            beanNameSet.remove(beanName);
            if (beanNameSet.isEmpty()) {
                this.prototypesCurrentlyInCreation.remove();
            }
        }
    }

    @Override
    public void destroyBean(String beanName, Object beanInstance) {
        destroyBean(beanName, beanInstance, getMergedLocalBeanDefinition(beanName));
    }

    protected void destroyBean(String beanName, Object bean, RootBeanDefinition mbd) {
        new DisposableBeanAdapter(bean, beanName, mbd, getBeanPostProcessors(), getAccessControlContext()).destroy();
    }

    @Override
    public void destroyScopedBean(String beanName) {
        RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
        if (mbd.isSingleton() || mbd.isPrototype()) {
            throw new IllegalArgumentException("Bean name '" + beanName + "' does not correspond to an object in a mutable scope");
        }
        String scopeName = mbd.getScope();
        Scope scope = this.scopes.get(scopeName);
        if (scope == null) {
            throw new IllegalStateException("No Scope SPI registered for scope name '" + scopeName + "'");
        }
        Object bean = scope.remove(beanName);
        if (bean != null) {
            destroyBean(beanName, bean, mbd);
        }
    }
    //---------------------------------------------------------------------
    // Implementation methods
    //---------------------------------------------------------------------

    protected String transformedBeanName(String name) {
        return canonicalName(BeanFactoryUtils.transformedBeanName(name));
    }

    protected String originalBeanName(String name) {
        String beanName = transformedBeanName(name);
        if (name.startsWith(FACTORY_BEAN_PREFIX)) {
            beanName = FACTORY_BEAN_PREFIX + beanName;
        }
        return beanName;
    }

    protected void initBeanWrapper(BeanWrapper bw) {
        bw.setConversionService(getConversionService());
        registerCustomEditors(bw);
    }

    protected void registerCustomEditors(PropertyEditorRegistry registry) {
        PropertyEditorRegistrySupport registrySupport = (registry instanceof PropertyEditorRegistrySupport ? (PropertyEditorRegistrySupport) registry : null);
        if (registrySupport != null) {
            registrySupport.useConfigValueEditors();
        }
        if (!this.propertyEditorRegistrars.isEmpty()) {
            for (PropertyEditorRegistrar registrar : this.propertyEditorRegistrars) {
                try {
                    registrar.registerCustomEditors(registry);
                } catch (BeanCreationException ex) {
                    Throwable rootCause = ex.getMostSpecificCause();
                    if (rootCause instanceof BeanCurrentlyInCreationException) {
                        BeanCreationException bce = (BeanCreationException) rootCause;
                        String bceBeanName = bce.getBeanName();
                        if (bceBeanName != null && isCurrentlyInCreation(bceBeanName)) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("PropertyEditorRegistrar [" + registrar.getClass().getName() + "] failed because it tried to obtain currently created bean '" + ex.getBeanName() + "': " + ex.getMessage());
                            }
                            onSuppressedException(ex);
                            continue;
                        }
                    }
                    throw ex;
                }
            }
        }
        if (!this.customEditors.isEmpty()) {
            this.customEditors.forEach((requiredType, editorClass) -> registry.registerCustomEditor(requiredType, BeanUtils.instantiateClass(editorClass)));
        }
    }

    protected RootBeanDefinition getMergedLocalBeanDefinition(String beanName) throws BeansException {
        // Quick check on the concurrent map first, with minimal locking.
        RootBeanDefinition mbd = this.mergedBeanDefinitions.get(beanName);
        if (mbd != null) {
            return mbd;
        }
        return getMergedBeanDefinition(beanName, getBeanDefinition(beanName));
    }

    protected RootBeanDefinition getMergedBeanDefinition(String beanName, BeanDefinition bd) throws BeanDefinitionStoreException {
        return getMergedBeanDefinition(beanName, bd, null);
    }

    protected RootBeanDefinition getMergedBeanDefinition(String beanName, BeanDefinition bd, @Nullable BeanDefinition containingBd) throws BeanDefinitionStoreException {
        synchronized (this.mergedBeanDefinitions) {
            RootBeanDefinition mbd = null;
            // Check with full lock now in order to enforce the same merged instance.
            if (containingBd == null) {
                mbd = this.mergedBeanDefinitions.get(beanName);
            }
            if (mbd == null) {
                if (bd.getParentName() == null) {
                    // Use copy of given root bean definition.
                    if (bd instanceof RootBeanDefinition) {
                        mbd = ((RootBeanDefinition) bd).cloneBeanDefinition();
                    } else {
                        mbd = new RootBeanDefinition(bd);
                    }
                } else {
                    // Child bean definition: needs to be merged with parent.
                    BeanDefinition pbd;
                    try {
                        String parentBeanName = transformedBeanName(bd.getParentName());
                        if (!beanName.equals(parentBeanName)) {
                            pbd = getMergedBeanDefinition(parentBeanName);
                        } else {
                            BeanFactory parent = getParentBeanFactory();
                            if (parent instanceof ConfigurableBeanFactory) {
                                pbd = ((ConfigurableBeanFactory) parent).getMergedBeanDefinition(parentBeanName);
                            } else {
                                throw new NoSuchBeanDefinitionException(parentBeanName, "Parent name '" + parentBeanName + "' is equal to bean name '" + beanName + "': cannot be resolved without an AbstractBeanFactory parent");
                            }
                        }
                    } catch (NoSuchBeanDefinitionException ex) {
                        throw new BeanDefinitionStoreException(bd.getResourceDescription(), beanName, "Could not resolve parent bean definition '" + bd.getParentName() + "'", ex);
                    }
                    // Deep copy with overridden values.
                    mbd = new RootBeanDefinition(pbd);
                    mbd.overrideFrom(bd);
                }
                // Set default singleton scope, if not configured before.
                if (!StringUtils.hasLength(mbd.getScope())) {
                    mbd.setScope(RootBeanDefinition.SCOPE_SINGLETON);
                }
                // A bean contained in a non-singleton bean cannot be a singleton itself.
                // Let's correct this on the fly here, since this might be the result of
                // parent-child merging for the outer bean, in which case the original inner bean
                // definition will not have inherited the merged outer bean's singleton status.
                if (containingBd != null && !containingBd.isSingleton() && mbd.isSingleton()) {
                    mbd.setScope(containingBd.getScope());
                }
                // Cache the merged bean definition for the time being
                // (it might still get re-merged later on in order to pick up metadata changes)
                if (containingBd == null && isCacheBeanMetadata()) {
                    this.mergedBeanDefinitions.put(beanName, mbd);
                }
            }
            return mbd;
        }
    }

    protected void checkMergedBeanDefinition(RootBeanDefinition mbd, String beanName, @Nullable Object[] args) throws BeanDefinitionStoreException {
        if (mbd.isAbstract()) {
            throw new BeanIsAbstractException(beanName);
        }
    }

    protected void clearMergedBeanDefinition(String beanName) {
        this.mergedBeanDefinitions.remove(beanName);
    }

    public void clearMetadataCache() {
        this.mergedBeanDefinitions.keySet().removeIf(bean -> !isBeanEligibleForMetadataCaching(bean));
    }

    @Nullable
    protected Class<?> resolveBeanClass(final RootBeanDefinition mbd, String beanName, final Class<?>... typesToMatch) throws CannotLoadBeanClassException {
        try {
            if (mbd.hasBeanClass()) {
                return mbd.getBeanClass();
            }
            if (System.getSecurityManager() != null) {
                return AccessController.doPrivileged((PrivilegedExceptionAction<Class<?>>) () -> doResolveBeanClass(mbd, typesToMatch), getAccessControlContext());
            } else {
                return doResolveBeanClass(mbd, typesToMatch);
            }
        } catch (PrivilegedActionException pae) {
            ClassNotFoundException ex = (ClassNotFoundException) pae.getException();
            throw new CannotLoadBeanClassException(mbd.getResourceDescription(), beanName, mbd.getBeanClassName(), ex);
        } catch (ClassNotFoundException ex) {
            throw new CannotLoadBeanClassException(mbd.getResourceDescription(), beanName, mbd.getBeanClassName(), ex);
        } catch (LinkageError err) {
            throw new CannotLoadBeanClassException(mbd.getResourceDescription(), beanName, mbd.getBeanClassName(), err);
        }
    }


    /**
     * 选择不同的ClassLoader加载类，beanClassLoader和tempClassLoader，和AspectJ有关
     */
    @Nullable
    private Class<?> doResolveBeanClass(RootBeanDefinition mbd, Class<?>... typesToMatch) throws ClassNotFoundException {
        ClassLoader beanClassLoader = getBeanClassLoader();
        ClassLoader dynamicLoader = beanClassLoader;
        boolean freshResolve = false;
        // 如果不为空
        if (!ObjectUtils.isEmpty(typesToMatch)) {
            /*
             * 只是类型检查的时候（不创建实例），使用这个暂时的Classloader(织入)
             * 一个ClassLoader对一个Class只加载一次，防止LoadTimeWeaver还没执行，类就被加载了，没被AOP拦截增强
             * tempClassLoader设置：beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
             * 时机：
             *      Xml：AbstractApplicationContext#prepareBeanFactory
             *      Annotation：AbstractApplicationContext#invokeBeanFactoryPostProcessors
             */
            ClassLoader tempClassLoader = getTempClassLoader();
            if (tempClassLoader != null) {
                dynamicLoader = tempClassLoader;
                freshResolve = true;
                if (tempClassLoader instanceof DecoratingClassLoader) {
                    DecoratingClassLoader dcl = (DecoratingClassLoader) tempClassLoader;
                    for (Class<?> typeToMatch : typesToMatch) {
                        dcl.excludeClass(typeToMatch.getName());
                    }
                }
            }
        }
        String className = mbd.getBeanClassName();
        if (className != null) {
            Object evaluated = evaluateBeanDefinitionString(className, mbd);
            if (!className.equals(evaluated)) {
                // A dynamically resolved expression, supported as of 4.2...
                if (evaluated instanceof Class) {
                    return (Class<?>) evaluated;
                } else if (evaluated instanceof String) {
                    className = (String) evaluated;
                    freshResolve = true;
                } else {
                    throw new IllegalStateException("Invalid class name expression result: " + evaluated);
                }
            }
            if (freshResolve) {
                // When resolving against a temporary class loader, exit early in order
                // to avoid storing the resolved Class in the bean definition.
                if (dynamicLoader != null) {
                    try {
                        return dynamicLoader.loadClass(className);
                    } catch (ClassNotFoundException ex) {
                        if (logger.isTraceEnabled()) {
                            logger.trace("Could not load class [" + className + "] from " + dynamicLoader + ": " + ex);
                        }
                    }
                }
                return ClassUtils.forName(className, dynamicLoader);
            }
        }
        // Resolve regularly, caching the result in the BeanDefinition...
        return mbd.resolveBeanClass(beanClassLoader);
    }

    @Nullable
    protected Object evaluateBeanDefinitionString(@Nullable String value, @Nullable BeanDefinition beanDefinition) {
        if (this.beanExpressionResolver == null) {
            return value;
        }
        Scope scope = null;
        if (beanDefinition != null) {
            String scopeName = beanDefinition.getScope();
            if (scopeName != null) {
                scope = getRegisteredScope(scopeName);
            }
        }
        return this.beanExpressionResolver.evaluate(value, new BeanExpressionContext(this, scope));
    }

    @Nullable
    protected Class<?> predictBeanType(String beanName, RootBeanDefinition mbd, Class<?>... typesToMatch) {
        Class<?> targetType = mbd.getTargetType();
        if (targetType != null) {
            return targetType;
        }
        if (mbd.getFactoryMethodName() != null) {
            return null;
        }
        return resolveBeanClass(mbd, beanName, typesToMatch);
    }

    protected boolean isFactoryBean(String beanName, RootBeanDefinition mbd) {
        Class<?> beanType = predictBeanType(beanName, mbd, FactoryBean.class);
        return (beanType != null && FactoryBean.class.isAssignableFrom(beanType));
    }

    @Nullable
    protected Class<?> getTypeForFactoryBean(String beanName, RootBeanDefinition mbd) {
        if (!mbd.isSingleton()) {
            return null;
        }
        try {
            FactoryBean<?> factoryBean = doGetBean(FACTORY_BEAN_PREFIX + beanName, FactoryBean.class, null, true);
            return getTypeForFactoryBean(factoryBean);
        } catch (BeanCreationException ex) {
            if (ex.contains(BeanCurrentlyInCreationException.class)) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Bean currently in creation on FactoryBean type check: " + ex);
                }
            } else if (mbd.isLazyInit()) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Bean creation exception on lazy FactoryBean type check: " + ex);
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Bean creation exception on non-lazy FactoryBean type check: " + ex);
                }
            }
            onSuppressedException(ex);
            return null;
        }
    }

    protected void markBeanAsCreated(String beanName) {
        if (!this.alreadyCreated.contains(beanName)) {
            synchronized (this.mergedBeanDefinitions) {
                if (!this.alreadyCreated.contains(beanName)) {
                    // Let the bean definition get re-merged now that we're actually creating
                    // the bean... just in case some of its metadata changed in the meantime.
                    clearMergedBeanDefinition(beanName);
                    this.alreadyCreated.add(beanName);
                }
            }
        }
    }

    protected void cleanupAfterBeanCreationFailure(String beanName) {
        synchronized (this.mergedBeanDefinitions) {
            this.alreadyCreated.remove(beanName);
        }
    }

    protected boolean isBeanEligibleForMetadataCaching(String beanName) {
        return this.alreadyCreated.contains(beanName);
    }

    protected boolean removeSingletonIfCreatedForTypeCheckOnly(String beanName) {
        if (!this.alreadyCreated.contains(beanName)) {
            removeSingleton(beanName);
            return true;
        } else {
            return false;
        }
    }

    protected boolean hasBeanCreationStarted() {
        return !this.alreadyCreated.isEmpty();
    }

    /**
     * 以&开头，如果beanInstance不是FactoryBean类型，抛异常
     * 以&开头，如果beanInstance是FactoryBean类型，直接返回beanInstance
     * 不以&开头，如果beanInstance不是FactoryBean类型，直接返回beanInstance
     * 不以&开头，如果beanInstance是FactoryBean类型
     *      单例：先从factoryBeanObjectCache获取，为空则调用FactoryBean#getObject创建，并放入factoryBeanObjectCache
     *      原型：不加入factoryBeanObjectCache，每次都重新创建
     */
    protected Object getObjectForBeanInstance(/*实例*/Object beanInstance, /*输入的名字*/String name, /*原名*/String beanName, @Nullable RootBeanDefinition mbd) {
        // 以&开头（想直接引用FactoryBean）
        if (BeanFactoryUtils.isFactoryDereference(name)) {
            if (beanInstance instanceof NullBean) {
                return beanInstance;
            }
            // 如果Bean不是FactoryBean，不让调用代码间接引用Factory
            if (!(beanInstance instanceof FactoryBean)) {
                throw new BeanIsNotAFactoryException(transformedBeanName(name), beanInstance.getClass());
            }
        }
        // （不是FactoryBean） || （以&开头 && FactoryBean）
        if (!(beanInstance instanceof FactoryBean) || BeanFactoryUtils.isFactoryDereference(name)) {
            // 直接返回
            return beanInstance;
        }
        // 不以&开头 && FactoryBean
        Object object = null;
        if (mbd == null) {
            // 从factoryBeanObjectCache获取
            object = getCachedObjectForFactoryBean(beanName);
        }
        if (object == null) {
            FactoryBean<?> factory = (FactoryBean<?>) beanInstance;
            // mbd == null && beanDefinitionMap包含beanName
            if (mbd == null && containsBeanDefinition(beanName)) {
                // 从mergedBeanDefinitions获取
                mbd = getMergedLocalBeanDefinition(beanName);
            }
            boolean synthetic = (mbd != null && mbd.isSynthetic());
            /*
             * 单例：先从factoryBeanObjectCache获取，为空则调用FactoryBean#getObject创建，并放入factoryBeanObjectCache
             * 原型：每次都创建新的
             */
            object = getObjectFromFactoryBean(factory, beanName, !synthetic);
        }
        return object;
    }

    public boolean isBeanNameInUse(String beanName) {
        return isAlias(beanName) || containsLocalBean(beanName) || hasDependentBean(beanName);
    }

    protected boolean requiresDestruction(Object bean, RootBeanDefinition mbd) {
        return (bean.getClass() != NullBean.class && (DisposableBeanAdapter.hasDestroyMethod(bean, mbd) || (hasDestructionAwareBeanPostProcessors() && DisposableBeanAdapter.hasApplicableProcessors(bean, getBeanPostProcessors()))));
    }

    protected void registerDisposableBeanIfNecessary(String beanName, Object bean, RootBeanDefinition mbd) {
        AccessControlContext acc = (System.getSecurityManager() != null ? getAccessControlContext() : null);
        if (!mbd.isPrototype() && requiresDestruction(bean, mbd)) {
            if (mbd.isSingleton()) {
                // Register a DisposableBean implementation that performs all destruction
                // work for the given bean: DestructionAwareBeanPostProcessors,
                // DisposableBean interface, custom destroy method.
                registerDisposableBean(beanName, new DisposableBeanAdapter(bean, beanName, mbd, getBeanPostProcessors(), acc));
            } else {
                // A bean with a custom scope...
                Scope scope = this.scopes.get(mbd.getScope());
                if (scope == null) {
                    throw new IllegalStateException("No Scope registered for scope name '" + mbd.getScope() + "'");
                }
                scope.registerDestructionCallback(beanName, new DisposableBeanAdapter(bean, beanName, mbd, getBeanPostProcessors(), acc));
            }
        }
    }
    //---------------------------------------------------------------------
    // Abstract methods to be implemented by subclasses
    //---------------------------------------------------------------------

    protected abstract boolean containsBeanDefinition(String beanName);

    protected abstract BeanDefinition getBeanDefinition(String beanName) throws BeansException;

    protected abstract Object createBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args) throws BeanCreationException;

}
