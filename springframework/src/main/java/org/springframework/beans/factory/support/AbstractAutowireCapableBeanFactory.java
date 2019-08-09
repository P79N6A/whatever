package org.springframework.beans.factory.support;

import org.apache.commons.logging.Log;
import org.springframework.beans.*;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.config.*;
import org.springframework.core.*;
import org.springframework.lang.Nullable;
import org.springframework.util.*;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

public abstract class AbstractAutowireCapableBeanFactory extends AbstractBeanFactory implements AutowireCapableBeanFactory {

    private InstantiationStrategy instantiationStrategy = new CglibSubclassingInstantiationStrategy();

    @Nullable
    private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    private boolean allowCircularReferences = true;

    private boolean allowRawInjectionDespiteWrapping = false;

    private final Set<Class<?>> ignoredDependencyTypes = new HashSet<>();

    private final Set<Class<?>> ignoredDependencyInterfaces = new HashSet<>();

    private final NamedThreadLocal<String> currentlyCreatedBean = new NamedThreadLocal<>("Currently created bean");

    private final ConcurrentMap<String, BeanWrapper> factoryBeanInstanceCache = new ConcurrentHashMap<>();

    private final ConcurrentMap<Class<?>, Method[]> factoryMethodCandidateCache = new ConcurrentHashMap<>();

    private final ConcurrentMap<Class<?>, PropertyDescriptor[]> filteredPropertyDescriptorsCache = new ConcurrentHashMap<>();

    public AbstractAutowireCapableBeanFactory() {
        super();
        ignoreDependencyInterface(BeanNameAware.class);
        ignoreDependencyInterface(BeanFactoryAware.class);
        ignoreDependencyInterface(BeanClassLoaderAware.class);
    }

    public AbstractAutowireCapableBeanFactory(@Nullable BeanFactory parentBeanFactory) {
        this();
        setParentBeanFactory(parentBeanFactory);
    }

    public void setInstantiationStrategy(InstantiationStrategy instantiationStrategy) {
        this.instantiationStrategy = instantiationStrategy;
    }

    protected InstantiationStrategy getInstantiationStrategy() {
        return this.instantiationStrategy;
    }

    public void setParameterNameDiscoverer(@Nullable ParameterNameDiscoverer parameterNameDiscoverer) {
        this.parameterNameDiscoverer = parameterNameDiscoverer;
    }

    @Nullable
    protected ParameterNameDiscoverer getParameterNameDiscoverer() {
        return this.parameterNameDiscoverer;
    }

    public void setAllowCircularReferences(boolean allowCircularReferences) {
        this.allowCircularReferences = allowCircularReferences;
    }

    public void setAllowRawInjectionDespiteWrapping(boolean allowRawInjectionDespiteWrapping) {
        this.allowRawInjectionDespiteWrapping = allowRawInjectionDespiteWrapping;
    }

    public void ignoreDependencyType(Class<?> type) {
        this.ignoredDependencyTypes.add(type);
    }

    public void ignoreDependencyInterface(Class<?> ifc) {
        this.ignoredDependencyInterfaces.add(ifc);
    }

    @Override
    public void copyConfigurationFrom(ConfigurableBeanFactory otherFactory) {
        super.copyConfigurationFrom(otherFactory);
        if (otherFactory instanceof AbstractAutowireCapableBeanFactory) {
            AbstractAutowireCapableBeanFactory otherAutowireFactory = (AbstractAutowireCapableBeanFactory) otherFactory;
            this.instantiationStrategy = otherAutowireFactory.instantiationStrategy;
            this.allowCircularReferences = otherAutowireFactory.allowCircularReferences;
            this.ignoredDependencyTypes.addAll(otherAutowireFactory.ignoredDependencyTypes);
            this.ignoredDependencyInterfaces.addAll(otherAutowireFactory.ignoredDependencyInterfaces);
        }
    }
    //-------------------------------------------------------------------------
    // Typical methods for creating and populating external bean instances
    //-------------------------------------------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    public <T> T createBean(Class<T> beanClass) throws BeansException {
        // Use prototype bean definition, to avoid registering bean as dependent bean.
        RootBeanDefinition bd = new RootBeanDefinition(beanClass);
        bd.setScope(SCOPE_PROTOTYPE);
        bd.allowCaching = ClassUtils.isCacheSafe(beanClass, getBeanClassLoader());
        return (T) createBean(beanClass.getName(), bd, null);
    }

    @Override
    public void autowireBean(Object existingBean) {
        // Use non-singleton bean definition, to avoid registering bean as dependent bean.
        RootBeanDefinition bd = new RootBeanDefinition(ClassUtils.getUserClass(existingBean));
        bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
        bd.allowCaching = ClassUtils.isCacheSafe(bd.getBeanClass(), getBeanClassLoader());
        BeanWrapper bw = new BeanWrapperImpl(existingBean);
        initBeanWrapper(bw);
        populateBean(bd.getBeanClass().getName(), bd, bw);
    }

    @Override
    public Object configureBean(Object existingBean, String beanName) throws BeansException {
        markBeanAsCreated(beanName);
        BeanDefinition mbd = getMergedBeanDefinition(beanName);
        RootBeanDefinition bd = null;
        if (mbd instanceof RootBeanDefinition) {
            RootBeanDefinition rbd = (RootBeanDefinition) mbd;
            bd = (rbd.isPrototype() ? rbd : rbd.cloneBeanDefinition());
        }
        if (bd == null) {
            bd = new RootBeanDefinition(mbd);
        }
        if (!bd.isPrototype()) {
            bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
            bd.allowCaching = ClassUtils.isCacheSafe(ClassUtils.getUserClass(existingBean), getBeanClassLoader());
        }
        BeanWrapper bw = new BeanWrapperImpl(existingBean);
        initBeanWrapper(bw);
        populateBean(beanName, bd, bw);
        return initializeBean(beanName, existingBean, bd);
    }
    //-------------------------------------------------------------------------
    // Specialized methods for fine-grained control over the bean lifecycle
    //-------------------------------------------------------------------------

    @Override
    public Object createBean(Class<?> beanClass, int autowireMode, boolean dependencyCheck) throws BeansException {
        // Use non-singleton bean definition, to avoid registering bean as dependent bean.
        RootBeanDefinition bd = new RootBeanDefinition(beanClass, autowireMode, dependencyCheck);
        bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
        return createBean(beanClass.getName(), bd, null);
    }

    @Override
    public Object autowire(Class<?> beanClass, int autowireMode, boolean dependencyCheck) throws BeansException {
        // Use non-singleton bean definition, to avoid registering bean as dependent bean.
        final RootBeanDefinition bd = new RootBeanDefinition(beanClass, autowireMode, dependencyCheck);
        bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
        if (bd.getResolvedAutowireMode() == AUTOWIRE_CONSTRUCTOR) {
            return autowireConstructor(beanClass.getName(), bd, null, null).getWrappedInstance();
        } else {
            Object bean;
            final BeanFactory parent = this;
            if (System.getSecurityManager() != null) {
                bean = AccessController.doPrivileged((PrivilegedAction<Object>) () -> getInstantiationStrategy().instantiate(bd, null, parent), getAccessControlContext());
            } else {
                bean = getInstantiationStrategy().instantiate(bd, null, parent);
            }
            populateBean(beanClass.getName(), bd, new BeanWrapperImpl(bean));
            return bean;
        }
    }

    @Override
    public void autowireBeanProperties(Object existingBean, int autowireMode, boolean dependencyCheck) throws BeansException {
        if (autowireMode == AUTOWIRE_CONSTRUCTOR) {
            throw new IllegalArgumentException("AUTOWIRE_CONSTRUCTOR not supported for existing bean instance");
        }
        // Use non-singleton bean definition, to avoid registering bean as dependent bean.
        RootBeanDefinition bd = new RootBeanDefinition(ClassUtils.getUserClass(existingBean), autowireMode, dependencyCheck);
        bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
        BeanWrapper bw = new BeanWrapperImpl(existingBean);
        initBeanWrapper(bw);
        populateBean(bd.getBeanClass().getName(), bd, bw);
    }

    @Override
    public void applyBeanPropertyValues(Object existingBean, String beanName) throws BeansException {
        markBeanAsCreated(beanName);
        BeanDefinition bd = getMergedBeanDefinition(beanName);
        BeanWrapper bw = new BeanWrapperImpl(existingBean);
        initBeanWrapper(bw);
        applyPropertyValues(beanName, bd, bw, bd.getPropertyValues());
    }

    @Override
    public Object initializeBean(Object existingBean, String beanName) {
        return initializeBean(beanName, existingBean, null);
    }

    @Override
    public Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName) throws BeansException {
        Object result = existingBean;
        for (BeanPostProcessor processor : getBeanPostProcessors()) {
            Object current = processor.postProcessBeforeInitialization(result, beanName);
            if (current == null) {
                return result;
            }
            result = current;
        }
        return result;
    }

    @Override
    public Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName) throws BeansException {
        Object result = existingBean;
        // beanPostProcessors
        for (BeanPostProcessor processor : getBeanPostProcessors()) {
            // BeanPostProcessor#postProcessAfterInitialization
            Object current = processor.postProcessAfterInitialization(result, beanName);
            if (current == null) {
                return result;
            }
            result = current;
        }
        return result;
    }

    @Override
    public void destroyBean(Object existingBean) {
        new DisposableBeanAdapter(existingBean, getBeanPostProcessors(), getAccessControlContext()).destroy();
    }
    //-------------------------------------------------------------------------
    // Delegate methods for resolving injection points
    //-------------------------------------------------------------------------

    @Override
    public Object resolveBeanByName(String name, DependencyDescriptor descriptor) {
        InjectionPoint previousInjectionPoint = ConstructorResolver.setCurrentInjectionPoint(descriptor);
        try {
            return getBean(name, descriptor.getDependencyType());
        } finally {
            ConstructorResolver.setCurrentInjectionPoint(previousInjectionPoint);
        }
    }

    @Override
    @Nullable
    public Object resolveDependency(DependencyDescriptor descriptor, @Nullable String requestingBeanName) throws BeansException {
        return resolveDependency(descriptor, requestingBeanName, null, null);
    }
    //---------------------------------------------------------------------
    // Implementation of relevant AbstractBeanFactory template methods
    //---------------------------------------------------------------------

    /**
     * 创建实例
     */
    @Override
    protected Object createBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args) throws BeanCreationException {
        if (logger.isTraceEnabled()) {
            logger.trace("Creating instance of bean '" + beanName + "'");
        }
        RootBeanDefinition mbdToUse = mbd;
        // 确保BeanDefinition中的Class被加载，克隆BeanDefinition，预防动态resolved的类不能存放在共享的MergedBeanDefinition
        Class<?> resolvedClass = resolveBeanClass(mbd, beanName);
        if (resolvedClass != null && !mbd.hasBeanClass() && mbd.getBeanClassName() != null) {
            mbdToUse = new RootBeanDefinition(mbd);
            mbdToUse.setBeanClass(resolvedClass);
        }
        // 准备方法覆写MethodOverrides，来自<lookup-method/>和<replaced-method/>
        try {
            mbdToUse.prepareMethodOverrides();
        } catch (BeanDefinitionValidationException ex) {
            throw new BeanDefinitionStoreException(mbdToUse.getResourceDescription(), beanName, "Validation of method overrides failed", ex);
        }
        try {
            /*
             * 遍历已实例化的BeanPostProcessor，如果是InstantiationAwareBeanPostProcessor类型，调用postProcessBeforeInstantiation
             * 如果上一步返回了Bean实例（代理），遍历已实例化的BeanPostProcessor，调用postProcessAfterInitialization，直接返回Bean，不再处理
             */
            Object bean = resolveBeforeInstantiation(beanName, mbdToUse);
            if (bean != null) {
                return bean;
            }
        } catch (Throwable ex) {
            throw new BeanCreationException(mbdToUse.getResourceDescription(), beanName, "BeanPostProcessor before instantiation of bean failed", ex);
        }
        try {
            // 创建Bean
            Object beanInstance = doCreateBean(beanName, mbdToUse, args);
            if (logger.isTraceEnabled()) {
                logger.trace("Finished creating instance of bean '" + beanName + "'");
            }
            return beanInstance;
        } catch (BeanCreationException | ImplicitlyAppearedSingletonException ex) {
            // A previously detected exception with proper bean creation context already,
            // or illegal singleton state to be communicated up to DefaultSingletonBeanRegistry.
            throw ex;
        } catch (Throwable ex) {
            throw new BeanCreationException(mbdToUse.getResourceDescription(), beanName, "Unexpected exception during bean creation", ex);
        }
    }

    /**
     * createBeanInstance 创建Bean实例
     * addSingletonFactory 将初始化的Bean提前暴露，处理单例的非构造函数的循环引用
     * populateBean 依赖注入
     * initializeBean 回调，applyBeanPostProcessorsBeforeInitialization - init-method - applyBeanPostProcessorsAfterInitialization
     */
    protected Object doCreateBean(final String beanName, final RootBeanDefinition mbd, final @Nullable Object[] args) throws BeanCreationException {
        BeanWrapper instanceWrapper = null;
        if (mbd.isSingleton()) {
            /*
             * 如果是单例先移除factoryBeanInstanceCache，猜测是FactoryBean类型匹配时生成的，不管它
             */
            instanceWrapper = this.factoryBeanInstanceCache.remove(beanName);
        }
        if (instanceWrapper == null) {
            /*
             * 实例化Bean：
             *      通过Supplier实例化
             *      通过FactoryMethod实例化
             *      通过含参构造函数实例化，依赖注入
             *          遍历已实例化的BeanPostProcessor，如果是SmartInstantiationAwareBeanPostProcessor类型，调用determineCandidateConstructors判断构造函数
             *      通过无参构造函数实例化
             */
            instanceWrapper = createBeanInstance(beanName, mbd, args);
        }
        // Bean实例
        final Object bean = instanceWrapper.getWrappedInstance();
        // 类型
        Class<?> beanType = instanceWrapper.getWrappedClass();
        if (beanType != NullBean.class) {
            mbd.resolvedTargetType = beanType;
        }
        synchronized (mbd.postProcessingLock) {
            if (!mbd.postProcessed) {
                try {
                    // 遍历已实例化的BeanPostProcessor，如果是MergedBeanDefinitionPostProcessor类型，调用postProcessMergedBeanDefinition
                    applyMergedBeanDefinitionPostProcessors(mbd, beanType, beanName);
                } catch (Throwable ex) {
                    throw new BeanCreationException(mbd.getResourceDescription(), beanName, "Post-processing of merged bean definition failed", ex);
                }
                mbd.postProcessed = true;
            }
        }
        // 单例 && 允许循环引用 && 当前beanName在singletonsCurrentlyInCreation中
        boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences && isSingletonCurrentlyInCreation(beanName));
        // 解决循环依赖，此时依赖未注入，提前暴露初始化的Bean，暴露一个ObjectFactory
        if (earlySingletonExposure) {
            if (logger.isTraceEnabled()) {
                logger.trace("Eagerly caching bean '" + beanName + "' to allow for resolving potential circular references");
            }
            /*
             * 如果this.singletonObjects不包含beanName（即初始化没完成）：
             *      将一个匿名的ObjectFactory实现放入this.singletonFactories，这个ObjectFactory包装了创建过程：
             *          遍历已实例化的BeanPostProcessor，如果是SmartInstantiationAwareBeanPostProcessor类型，调用getEarlyBeanReference，返回一个早期引用对象
             *          基本是什么也没干将Bean实例原样返回，但是给了AbstractAutoProxyCreator一个包装代理的机会
             *      从this.earlySingletonObjects移除beanName键和值（新的由ObjectFactory生成）
             *      添加beanName到this.registeredSingletons
             */
            addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
        }
        // 创建一个变量保存对象
        Object exposedObject = bean;
        try {
            /*
             * 属性注入：
             * 遍历已实例化的BeanPostProcessor，如果是InstantiationAwareBeanPostProcessor类型，调用postProcessAfterInstantiation处理Bean实例
             * 如果需要继续设置值，继续，否则直接返回
             * 按照装配类型，按名字或类型调用BeanFactory#getBean获取对应依赖
             * 遍历已实例化的BeanPostProcessor，如果是InstantiationAwareBeanPostProcessor类型，调用postProcessProperties及postProcessPropertyValues
             * 上面一步中AutowiredAnnotationBeanPostProcessor会对@Autowired、@Value、@Inject注解的依赖注入设值
             * 对Bean应用依赖，注入设值
             */
            populateBean(beanName, mbd, instanceWrapper);

            /*
             * 如果Bean实现了BeanNameAware、BeanClassLoaderAware或BeanFactoryAware接口，回调
             * 遍历已实例化添加的BeanPostProcessor，调用postProcessBeforeInitialization
             * 如果Bean实现了InitializingBean接口，调用afterPropertiesSet
             * 如果Bean定义了initMethod方法，反射调用initMethod
             * 遍历已实例化添加的BeanPostProcessor，调用postProcessAfterInitialization
             * AOP会在创建Bean实例最后进行代理增强
             */
            exposedObject = initializeBean(beanName, exposedObject, mbd);
        } catch (Throwable ex) {
            if (ex instanceof BeanCreationException && beanName.equals(((BeanCreationException) ex).getBeanName())) {
                throw (BeanCreationException) ex;
            } else {
                throw new BeanCreationException(mbd.getResourceDescription(), beanName, "Initialization of bean failed", ex);
            }
        }
        /*
         * 此时依赖已注入，初始化完成，处理之前提前暴露的早期单例
         */
        if (earlySingletonExposure) {
            /*
             * 按顺序查缓存：
             *      this.singletonObjects
             *      this.earlySingletonObjects
             *      this.singletonFactories(因为allowEarlyReference==false，所以不从这里查找)
             */
            Object earlySingletonReference = getSingleton(beanName, false);
            if (earlySingletonReference != null) {

                if (exposedObject == bean) {
                    // 返回早期引用
                    exposedObject = earlySingletonReference;
                } else if (!this.allowRawInjectionDespiteWrapping && hasDependentBean(beanName)) {
                    String[] dependentBeans = getDependentBeans(beanName);
                    Set<String> actualDependentBeans = new LinkedHashSet<>(dependentBeans.length);
                    for (String dependentBean : dependentBeans) {
                        if (!removeSingletonIfCreatedForTypeCheckOnly(dependentBean)) {
                            actualDependentBeans.add(dependentBean);
                        }
                    }
                    if (!actualDependentBeans.isEmpty()) {
                        throw new BeanCurrentlyInCreationException(beanName, "Bean with name '" + beanName + "' has been injected into other beans [" + StringUtils.collectionToCommaDelimitedString(actualDependentBeans) + "] in its raw version as part of a circular reference, but has eventually been " + "wrapped. This means that said other beans do not use the final version of the " + "bean. This is often the result of over-eager type matching - consider using " + "'getBeanNamesOfType' with the 'allowEagerInit' flag turned off, for example.");
                    }
                }
            }
        }
        try {
            // 放入disposableBeans
            registerDisposableBeanIfNecessary(beanName, bean, mbd);
        } catch (BeanDefinitionValidationException ex) {
            throw new BeanCreationException(mbd.getResourceDescription(), beanName, "Invalid destruction signature", ex);
        }
        return exposedObject;
    }

    @Override
    @Nullable
    protected Class<?> predictBeanType(String beanName, RootBeanDefinition mbd, Class<?>... typesToMatch) {
        Class<?> targetType = determineTargetType(beanName, mbd, typesToMatch);
        // Apply SmartInstantiationAwareBeanPostProcessors to predict the
        // eventual type after a before-instantiation shortcut.
        if (targetType != null && !mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
            for (BeanPostProcessor bp : getBeanPostProcessors()) {
                if (bp instanceof SmartInstantiationAwareBeanPostProcessor) {
                    SmartInstantiationAwareBeanPostProcessor ibp = (SmartInstantiationAwareBeanPostProcessor) bp;
                    Class<?> predicted = ibp.predictBeanType(targetType, beanName);
                    if (predicted != null && (typesToMatch.length != 1 || FactoryBean.class != typesToMatch[0] || FactoryBean.class.isAssignableFrom(predicted))) {
                        return predicted;
                    }
                }
            }
        }
        return targetType;
    }

    @Nullable
    protected Class<?> determineTargetType(String beanName, RootBeanDefinition mbd, Class<?>... typesToMatch) {
        Class<?> targetType = mbd.getTargetType();
        if (targetType == null) {
            targetType = (mbd.getFactoryMethodName() != null ? getTypeForFactoryMethod(beanName, mbd, typesToMatch) : resolveBeanClass(mbd, beanName, typesToMatch));
            if (ObjectUtils.isEmpty(typesToMatch) || getTempClassLoader() == null) {
                mbd.resolvedTargetType = targetType;
            }
        }
        return targetType;
    }

    @Nullable
    protected Class<?> getTypeForFactoryMethod(String beanName, RootBeanDefinition mbd, Class<?>... typesToMatch) {
        ResolvableType cachedReturnType = mbd.factoryMethodReturnType;
        if (cachedReturnType != null) {
            return cachedReturnType.resolve();
        }
        Class<?> commonType = null;
        Method uniqueCandidate = mbd.factoryMethodToIntrospect;
        if (uniqueCandidate == null) {
            Class<?> factoryClass;
            boolean isStatic = true;
            String factoryBeanName = mbd.getFactoryBeanName();
            if (factoryBeanName != null) {
                if (factoryBeanName.equals(beanName)) {
                    throw new BeanDefinitionStoreException(mbd.getResourceDescription(), beanName, "factory-bean reference points back to the same bean definition");
                }
                // Check declared factory method return type on factory class.
                factoryClass = getType(factoryBeanName);
                isStatic = false;
            } else {
                // Check declared factory method return type on bean class.
                factoryClass = resolveBeanClass(mbd, beanName, typesToMatch);
            }
            if (factoryClass == null) {
                return null;
            }
            factoryClass = ClassUtils.getUserClass(factoryClass);
            // If all factory methods have the same return type, return that type.
            // Can't clearly figure out exact method due to type converting / autowiring!
            int minNrOfArgs = (mbd.hasConstructorArgumentValues() ? mbd.getConstructorArgumentValues().getArgumentCount() : 0);
            Method[] candidates = this.factoryMethodCandidateCache.computeIfAbsent(factoryClass, clazz -> ReflectionUtils.getUniqueDeclaredMethods(clazz, ReflectionUtils.USER_DECLARED_METHODS));
            for (Method candidate : candidates) {
                if (Modifier.isStatic(candidate.getModifiers()) == isStatic && mbd.isFactoryMethod(candidate) && candidate.getParameterCount() >= minNrOfArgs) {
                    // Declared type variables to inspect?
                    if (candidate.getTypeParameters().length > 0) {
                        try {
                            // Fully resolve parameter names and argument values.
                            Class<?>[] paramTypes = candidate.getParameterTypes();
                            String[] paramNames = null;
                            ParameterNameDiscoverer pnd = getParameterNameDiscoverer();
                            if (pnd != null) {
                                paramNames = pnd.getParameterNames(candidate);
                            }
                            ConstructorArgumentValues cav = mbd.getConstructorArgumentValues();
                            Set<ConstructorArgumentValues.ValueHolder> usedValueHolders = new HashSet<>(paramTypes.length);
                            Object[] args = new Object[paramTypes.length];
                            for (int i = 0; i < args.length; i++) {
                                ConstructorArgumentValues.ValueHolder valueHolder = cav.getArgumentValue(i, paramTypes[i], (paramNames != null ? paramNames[i] : null), usedValueHolders);
                                if (valueHolder == null) {
                                    valueHolder = cav.getGenericArgumentValue(null, null, usedValueHolders);
                                }
                                if (valueHolder != null) {
                                    args[i] = valueHolder.getValue();
                                    usedValueHolders.add(valueHolder);
                                }
                            }
                            Class<?> returnType = AutowireUtils.resolveReturnTypeForFactoryMethod(candidate, args, getBeanClassLoader());
                            uniqueCandidate = (commonType == null && returnType == candidate.getReturnType() ? candidate : null);
                            commonType = ClassUtils.determineCommonAncestor(returnType, commonType);
                            if (commonType == null) {
                                // Ambiguous return types found: return null to indicate "not determinable".
                                return null;
                            }
                        } catch (Throwable ex) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Failed to resolve generic return type for factory method: " + ex);
                            }
                        }
                    } else {
                        uniqueCandidate = (commonType == null ? candidate : null);
                        commonType = ClassUtils.determineCommonAncestor(candidate.getReturnType(), commonType);
                        if (commonType == null) {
                            // Ambiguous return types found: return null to indicate "not determinable".
                            return null;
                        }
                    }
                }
            }
            mbd.factoryMethodToIntrospect = uniqueCandidate;
            if (commonType == null) {
                return null;
            }
        }
        // Common return type found: all factory methods return same type. For a non-parameterized
        // unique candidate, cache the full type declaration context of the target factory method.
        cachedReturnType = (uniqueCandidate != null ? ResolvableType.forMethodReturnType(uniqueCandidate) : ResolvableType.forClass(commonType));
        mbd.factoryMethodReturnType = cachedReturnType;
        return cachedReturnType.resolve();
    }

    @Override
    @Nullable
    protected Class<?> getTypeForFactoryBean(String beanName, RootBeanDefinition mbd) {
        if (mbd.getInstanceSupplier() != null) {
            ResolvableType targetType = mbd.targetType;
            if (targetType != null) {
                Class<?> result = targetType.as(FactoryBean.class).getGeneric().resolve();
                if (result != null) {
                    return result;
                }
            }
            if (mbd.hasBeanClass()) {
                Class<?> result = GenericTypeResolver.resolveTypeArgument(mbd.getBeanClass(), FactoryBean.class);
                if (result != null) {
                    return result;
                }
            }
        }
        String factoryBeanName = mbd.getFactoryBeanName();
        String factoryMethodName = mbd.getFactoryMethodName();
        if (factoryBeanName != null) {
            if (factoryMethodName != null) {
                // Try to obtain the FactoryBean's object type from its factory method declaration
                // without instantiating the containing bean at all.
                BeanDefinition fbDef = getBeanDefinition(factoryBeanName);
                if (fbDef instanceof AbstractBeanDefinition) {
                    AbstractBeanDefinition afbDef = (AbstractBeanDefinition) fbDef;
                    if (afbDef.hasBeanClass()) {
                        Class<?> result = getTypeForFactoryBeanFromMethod(afbDef.getBeanClass(), factoryMethodName);
                        if (result != null) {
                            return result;
                        }
                    }
                }
            }
            // If not resolvable above and the referenced factory bean doesn't exist yet,
            // exit here - we don't want to force the creation of another bean just to
            // obtain a FactoryBean's object type...
            if (!isBeanEligibleForMetadataCaching(factoryBeanName)) {
                return null;
            }
        }
        // Let's obtain a shortcut instance for an early getObjectType() call...
        FactoryBean<?> fb = (mbd.isSingleton() ? getSingletonFactoryBeanForTypeCheck(beanName, mbd) : getNonSingletonFactoryBeanForTypeCheck(beanName, mbd));
        if (fb != null) {
            // Try to obtain the FactoryBean's object type from this early stage of the instance.
            Class<?> result = getTypeForFactoryBean(fb);
            if (result != null) {
                return result;
            } else {
                // No type found for shortcut FactoryBean instance:
                // fall back to full creation of the FactoryBean instance.
                return super.getTypeForFactoryBean(beanName, mbd);
            }
        }
        if (factoryBeanName == null && mbd.hasBeanClass()) {
            // No early bean instantiation possible: determine FactoryBean's type from
            // static factory method signature or from class inheritance hierarchy...
            if (factoryMethodName != null) {
                return getTypeForFactoryBeanFromMethod(mbd.getBeanClass(), factoryMethodName);
            } else {
                return GenericTypeResolver.resolveTypeArgument(mbd.getBeanClass(), FactoryBean.class);
            }
        }
        return null;
    }

    @Nullable
    private Class<?> getTypeForFactoryBeanFromMethod(Class<?> beanClass, final String factoryMethodName) {
        class Holder {

            @Nullable
            Class<?> value = null;

        }
        final Holder objectType = new Holder();
        // CGLIB subclass methods hide generic parameters; look at the original user class.
        Class<?> fbClass = ClassUtils.getUserClass(beanClass);
        // Find the given factory method, taking into account that in the case of
        // @Bean methods, there may be parameters present.
        ReflectionUtils.doWithMethods(fbClass, method -> {
            if (method.getName().equals(factoryMethodName) && FactoryBean.class.isAssignableFrom(method.getReturnType())) {
                Class<?> currentType = GenericTypeResolver.resolveReturnTypeArgument(method, FactoryBean.class);
                if (currentType != null) {
                    objectType.value = ClassUtils.determineCommonAncestor(currentType, objectType.value);
                }
            }
        }, ReflectionUtils.USER_DECLARED_METHODS);
        return (objectType.value != null && Object.class != objectType.value ? objectType.value : null);
    }

    protected Object getEarlyBeanReference(String beanName, RootBeanDefinition mbd, Object bean) {
        Object exposedObject = bean;
        // hasInstantiationAwareBeanPostProcessors
        if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
            // beanPostProcessors
            for (BeanPostProcessor bp : getBeanPostProcessors()) {
                // SmartInstantiationAwareBeanPostProcessor
                if (bp instanceof SmartInstantiationAwareBeanPostProcessor) {
                    SmartInstantiationAwareBeanPostProcessor ibp = (SmartInstantiationAwareBeanPostProcessor) bp;
                    // SmartInstantiationAwareBeanPostProcessor#getEarlyBeanReference
                    exposedObject = ibp.getEarlyBeanReference(exposedObject, beanName);
                }
            }
        }
        return exposedObject;
    }
    //---------------------------------------------------------------------
    // Implementation methods
    //---------------------------------------------------------------------

    @Nullable
    private FactoryBean<?> getSingletonFactoryBeanForTypeCheck(String beanName, RootBeanDefinition mbd) {
        synchronized (getSingletonMutex()) {
            BeanWrapper bw = this.factoryBeanInstanceCache.get(beanName);
            if (bw != null) {
                return (FactoryBean<?>) bw.getWrappedInstance();
            }
            Object beanInstance = getSingleton(beanName, false);
            if (beanInstance instanceof FactoryBean) {
                return (FactoryBean<?>) beanInstance;
            }
            if (isSingletonCurrentlyInCreation(beanName) || (mbd.getFactoryBeanName() != null && isSingletonCurrentlyInCreation(mbd.getFactoryBeanName()))) {
                return null;
            }
            Object instance;
            try {
                // Mark this bean as currently in creation, even if just partially.
                beforeSingletonCreation(beanName);
                // Give BeanPostProcessors a chance to return a proxy instead of the target bean instance.
                instance = resolveBeforeInstantiation(beanName, mbd);
                if (instance == null) {
                    bw = createBeanInstance(beanName, mbd, null);
                    instance = bw.getWrappedInstance();
                }
            } catch (UnsatisfiedDependencyException ex) {
                // Don't swallow, probably misconfiguration...
                throw ex;
            } catch (BeanCreationException ex) {
                // Instantiation failure, maybe too early...
                if (logger.isDebugEnabled()) {
                    logger.debug("Bean creation exception on singleton FactoryBean type check: " + ex);
                }
                onSuppressedException(ex);
                return null;
            } finally {
                // Finished partial creation of this bean.
                afterSingletonCreation(beanName);
            }
            FactoryBean<?> fb = getFactoryBean(beanName, instance);
            if (bw != null) {
                this.factoryBeanInstanceCache.put(beanName, bw);
            }
            return fb;
        }
    }

    @Nullable
    private FactoryBean<?> getNonSingletonFactoryBeanForTypeCheck(String beanName, RootBeanDefinition mbd) {
        if (isPrototypeCurrentlyInCreation(beanName)) {
            return null;
        }
        Object instance;
        try {
            // Mark this bean as currently in creation, even if just partially.
            beforePrototypeCreation(beanName);
            // Give BeanPostProcessors a chance to return a proxy instead of the target bean instance.
            instance = resolveBeforeInstantiation(beanName, mbd);
            if (instance == null) {
                BeanWrapper bw = createBeanInstance(beanName, mbd, null);
                instance = bw.getWrappedInstance();
            }
        } catch (UnsatisfiedDependencyException ex) {
            // Don't swallow, probably misconfiguration...
            throw ex;
        } catch (BeanCreationException ex) {
            // Instantiation failure, maybe too early...
            if (logger.isDebugEnabled()) {
                logger.debug("Bean creation exception on non-singleton FactoryBean type check: " + ex);
            }
            onSuppressedException(ex);
            return null;
        } finally {
            // Finished partial creation of this bean.
            afterPrototypeCreation(beanName);
        }
        return getFactoryBean(beanName, instance);
    }

    protected void applyMergedBeanDefinitionPostProcessors(RootBeanDefinition mbd, Class<?> beanType, String beanName) {
        for (BeanPostProcessor bp : getBeanPostProcessors()) {
            if (bp instanceof MergedBeanDefinitionPostProcessor) {
                MergedBeanDefinitionPostProcessor bdp = (MergedBeanDefinitionPostProcessor) bp;
                bdp.postProcessMergedBeanDefinition(mbd, beanType, beanName);
            }
        }
    }

    @Nullable
    protected Object resolveBeforeInstantiation(String beanName, RootBeanDefinition mbd) {
        Object bean = null;
        if (!Boolean.FALSE.equals(mbd.beforeInstantiationResolved)) {
            // Make sure bean class is actually resolved at this point.
            if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
                Class<?> targetType = determineTargetType(beanName, mbd);
                if (targetType != null) {
                    bean = applyBeanPostProcessorsBeforeInstantiation(targetType, beanName);
                    if (bean != null) {
                        bean = applyBeanPostProcessorsAfterInitialization(bean, beanName);
                    }
                }
            }
            mbd.beforeInstantiationResolved = (bean != null);
        }
        return bean;
    }

    @Nullable
    protected Object applyBeanPostProcessorsBeforeInstantiation(Class<?> beanClass, String beanName) {
        // beanPostProcessors
        for (BeanPostProcessor bp : getBeanPostProcessors()) {
            if (bp instanceof InstantiationAwareBeanPostProcessor) {
                InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
                // InstantiationAwareBeanPostProcessor#postProcessBeforeInstantiation
                Object result = ibp.postProcessBeforeInstantiation(beanClass, beanName);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    protected BeanWrapper createBeanInstance(String beanName, RootBeanDefinition mbd, @Nullable Object[] args) {
        // 确保已经加载了class
        Class<?> beanClass = resolveBeanClass(mbd, beanName);
        // 校验类的访问权限
        if (beanClass != null && !Modifier.isPublic(beanClass.getModifiers()) && !mbd.isNonPublicAccessAllowed()) {
            throw new BeanCreationException(mbd.getResourceDescription(), beanName, "Bean class isn't public, and non-public access not allowed: " + beanClass.getName());
        }
        // instanceSupplier
        Supplier<?> instanceSupplier = mbd.getInstanceSupplier();
        if (instanceSupplier != null) {
            //  从Supplier获得
            return obtainFromSupplier(instanceSupplier, beanName);
        }
        // factoryMethodName
        if (mbd.getFactoryMethodName() != null) {
            // 工厂方法实例化
            return instantiateUsingFactoryMethod(beanName, mbd, args);
        }
        // 如果不是第一次创建，如Prototype，可以从第一次知道，采用无参构造函数，还是构造函数依赖注入完成实例化
        boolean resolved = false;
        // 是否需要通过构造函数注入
        boolean autowireNecessary = false;
        if (args == null) {
            synchronized (mbd.constructorArgumentLock) {
                if (mbd.resolvedConstructorOrFactoryMethod != null) {
                    // 已创建过了
                    resolved = true;
                    autowireNecessary = mbd.constructorArgumentsResolved;
                }
            }
        }
        if (resolved) {
            if (autowireNecessary) {
                // 构造函数依赖注入
                return autowireConstructor(beanName, mbd, null, null);
            } else {
                // 无参构造函数
                return instantiateBean(beanName, mbd);
            }
        }
        // 判断是否采用有参构造函数
        // 遍历已实例化的BeanPostProcessor，如果是SmartInstantiationAwareBeanPostProcessor类型，调用determineCandidateConstructors判断
        Constructor<?>[] ctors = determineConstructorsFromBeanPostProcessors(beanClass, beanName);
        if (ctors != null || mbd.getResolvedAutowireMode() == AUTOWIRE_CONSTRUCTOR || mbd.hasConstructorArgumentValues() || !ObjectUtils.isEmpty(args)) {
            // 构造函数依赖注入
            return autowireConstructor(beanName, mbd, ctors, args);
        }
        // Preferred constructors for default construction?
        ctors = mbd.getPreferredConstructors();
        if (ctors != null) {
            // 构造函数依赖注入
            return autowireConstructor(beanName, mbd, ctors, null);
        }
        // 调用无参构造函数
        return instantiateBean(beanName, mbd);
    }

    protected BeanWrapper obtainFromSupplier(Supplier<?> instanceSupplier, String beanName) {
        Object instance;
        String outerBean = this.currentlyCreatedBean.get();
        this.currentlyCreatedBean.set(beanName);
        try {
            instance = instanceSupplier.get();
        } finally {
            if (outerBean != null) {
                this.currentlyCreatedBean.set(outerBean);
            } else {
                this.currentlyCreatedBean.remove();
            }
        }
        if (instance == null) {
            instance = new NullBean();
        }
        BeanWrapper bw = new BeanWrapperImpl(instance);
        initBeanWrapper(bw);
        return bw;
    }

    @Override
    protected Object getObjectForBeanInstance(Object beanInstance, String name, String beanName, @Nullable RootBeanDefinition mbd) {
        String currentlyCreatedBean = this.currentlyCreatedBean.get();
        if (currentlyCreatedBean != null) {
            registerDependentBean(beanName, currentlyCreatedBean);
        }
        return super.getObjectForBeanInstance(beanInstance, name, beanName, mbd);
    }

    @Nullable
    protected Constructor<?>[] determineConstructorsFromBeanPostProcessors(@Nullable Class<?> beanClass, String beanName) throws BeansException {
        // hasInstantiationAwareBeanPostProcessors
        if (beanClass != null && hasInstantiationAwareBeanPostProcessors()) {
            // beanPostProcessors
            for (BeanPostProcessor bp : getBeanPostProcessors()) {
                // SmartInstantiationAwareBeanPostProcessor
                if (bp instanceof SmartInstantiationAwareBeanPostProcessor) {
                    SmartInstantiationAwareBeanPostProcessor ibp = (SmartInstantiationAwareBeanPostProcessor) bp;
                    Constructor<?>[] ctors = ibp.determineCandidateConstructors(beanClass, beanName);
                    if (ctors != null) {
                        return ctors;
                    }
                }
            }
        }
        return null;
    }

    protected BeanWrapper instantiateBean(final String beanName, final RootBeanDefinition mbd) {
        try {
            Object beanInstance;
            final BeanFactory parent = this;
            if (System.getSecurityManager() != null) {
                beanInstance = AccessController.doPrivileged((PrivilegedAction<Object>) () -> getInstantiationStrategy().instantiate(mbd, beanName, parent), getAccessControlContext());
            } else {
                // 根据初始化策略InstantiationStrategy初始化
                // instantiationStrategy 默认CglibSubclassingInstantiationStrategy
                beanInstance = getInstantiationStrategy().instantiate(mbd, beanName, parent);
            }
            // BeanWrapper包装
            BeanWrapper bw = new BeanWrapperImpl(beanInstance);
            // 初始化BeanWrapper：设置ConversionService及注册CustomEditors
            initBeanWrapper(bw);
            return bw;
        } catch (Throwable ex) {
            throw new BeanCreationException(mbd.getResourceDescription(), beanName, "Instantiation of bean failed", ex);
        }
    }

    protected BeanWrapper instantiateUsingFactoryMethod(String beanName, RootBeanDefinition mbd, @Nullable Object[] explicitArgs) {
        return new ConstructorResolver(this).instantiateUsingFactoryMethod(beanName, mbd, explicitArgs);
    }

    protected BeanWrapper autowireConstructor(String beanName, RootBeanDefinition mbd, @Nullable Constructor<?>[] ctors, @Nullable Object[] explicitArgs) {
        return new ConstructorResolver(this).autowireConstructor(beanName, mbd, ctors, explicitArgs);
    }

    @SuppressWarnings("deprecation")  // for postProcessPropertyValues
    protected void populateBean(String beanName, RootBeanDefinition mbd, @Nullable BeanWrapper bw) {
        if (bw == null) {
            if (mbd.hasPropertyValues()) {
                throw new BeanCreationException(mbd.getResourceDescription(), beanName, "Cannot apply property values to null instance");
            } else {
                // Skip property population phase for null instance.
                return;
            }
        }
        // 到这里，Bean实例化完成，但还没开始属性设值，InstantiationAwareBeanPostProcessor可以在这里修改Bean
        boolean continueWithPropertyPopulation = true;
        /*
         * 遍历已实例化的BeanPostProcessor，如果是InstantiationAwareBeanPostProcessor类型，调用postProcessAfterInstantiation
         * 如果需要继续设置值，继续
         */
        if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
            for (BeanPostProcessor bp : getBeanPostProcessors()) {
                if (bp instanceof InstantiationAwareBeanPostProcessor) {
                    InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
                    // 不需要经过其他的BeanPostProcessor处理
                    if (!ibp.postProcessAfterInstantiation(bw.getWrappedInstance(), beanName)) {
                        continueWithPropertyPopulation = false;
                        break;
                    }
                }
            }
        }
        // 不需要继续
        if (!continueWithPropertyPopulation) {
            return;
        }
        // 要设置的属性-值
        PropertyValues pvs = (mbd.hasPropertyValues() ? mbd.getPropertyValues() : null);
        // 注入模式
        int resolvedAutowireMode = mbd.getResolvedAutowireMode();
        if (resolvedAutowireMode == AUTOWIRE_BY_NAME || resolvedAutowireMode == AUTOWIRE_BY_TYPE) {
            // 总之先放进去
            MutablePropertyValues newPvs = new MutablePropertyValues(pvs);
            // 通过名字装配
            if (resolvedAutowireMode == AUTOWIRE_BY_NAME) {
                autowireByName(beanName, mbd, bw, newPvs);
            }
            // 通过类型装配
            if (resolvedAutowireMode == AUTOWIRE_BY_TYPE) {
                autowireByType(beanName, mbd, bw, newPvs);
            }
            pvs = newPvs;
        }
        // hasInstantiationAwareBeanPostProcessors
        boolean hasInstAwareBpps = hasInstantiationAwareBeanPostProcessors();
        boolean needsDepCheck = (mbd.getDependencyCheck() != AbstractBeanDefinition.DEPENDENCY_CHECK_NONE);
        PropertyDescriptor[] filteredPds = null;
        if (hasInstAwareBpps) {
            if (pvs == null) {
                pvs = mbd.getPropertyValues();
            }
            // 遍历已实例化的BeanPostProcessor，如果是InstantiationAwareBeanPostProcessor类型，调用postProcessProperties
            for (BeanPostProcessor bp : getBeanPostProcessors()) {
                if (bp instanceof InstantiationAwareBeanPostProcessor) {
                    InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
                    PropertyValues pvsToUse = ibp.postProcessProperties(pvs, bw.getWrappedInstance(), beanName);
                    if (pvsToUse == null) {
                        if (filteredPds == null) {
                            filteredPds = filterPropertyDescriptorsForDependencyCheck(bw, mbd.allowCaching);
                        }
                        // AutowiredAnnotationBeanPostProcessor对@Autowired、@Value、@Inject注解的依赖设值
                        pvsToUse = ibp.postProcessPropertyValues(pvs, filteredPds, bw.getWrappedInstance(), beanName);
                        // 注入依赖不成功，直接返回
                        if (pvsToUse == null) {
                            return;
                        }
                    }
                    pvs = pvsToUse;
                }
            }
        }
        if (needsDepCheck) {
            if (filteredPds == null) {
                filteredPds = filterPropertyDescriptorsForDependencyCheck(bw, mbd.allowCaching);
            }
            checkDependencies(beanName, mbd, filteredPds, pvs);
        }
        // 设置Bean实例的属性值
        if (pvs != null) {
            applyPropertyValues(beanName, mbd, bw, pvs);
        }
    }

    /**
     * 按名称注入
     */
    protected void autowireByName(String beanName, AbstractBeanDefinition mbd, BeanWrapper bw, MutablePropertyValues pvs) {
        String[] propertyNames = unsatisfiedNonSimpleProperties(mbd, bw);
        // 属性名
        for (String propertyName : propertyNames) {
            // 包含依赖
            if (containsBean(propertyName)) {
                // 获得依赖
                Object bean = getBean(propertyName);
                // 添加进去
                pvs.add(propertyName, bean);
                // 注册依赖 dependentBeanMap containedBeans dependenciesForBeanMap
                registerDependentBean(propertyName, beanName);
                if (logger.isTraceEnabled()) {
                    logger.trace("Added autowiring by name from bean name '" + beanName + "' via property '" + propertyName + "' to bean named '" + propertyName + "'");
                }
            }
            // 没找到
            else {
                if (logger.isTraceEnabled()) {
                    logger.trace("Not autowiring property '" + propertyName + "' of bean '" + beanName + "' by name: no matching bean found");
                }
            }
        }
    }

    /**
     * 按类型注入
     */
    protected void autowireByType(String beanName, AbstractBeanDefinition mbd, BeanWrapper bw, MutablePropertyValues pvs) {
        TypeConverter converter = getCustomTypeConverter();
        if (converter == null) {
            converter = bw;
        }
        Set<String> autowiredBeanNames = new LinkedHashSet<>(4);
        String[] propertyNames = unsatisfiedNonSimpleProperties(mbd, bw);
        // 属性名
        for (String propertyName : propertyNames) {
            try {
                PropertyDescriptor pd = bw.getPropertyDescriptor(propertyName);
                // Don't try autowiring by type for type Object: never makes sense,
                // even if it technically is a unsatisfied, non-simple property.
                if (Object.class != pd.getPropertyType()) {
                    MethodParameter methodParam = BeanUtils.getWriteMethodParameter(pd);
                    // Do not allow eager init for type matching in case of a prioritized post-processor.
                    boolean eager = !PriorityOrdered.class.isInstance(bw.getWrappedInstance());
                    DependencyDescriptor desc = new AutowireByTypeDependencyDescriptor(methodParam, eager);
                    // 获得依赖
                    Object autowiredArgument = resolveDependency(desc, beanName, autowiredBeanNames, converter);
                    if (autowiredArgument != null) {
                        // 添加进去
                        pvs.add(propertyName, autowiredArgument);
                    }
                    for (String autowiredBeanName : autowiredBeanNames) {
                        // 注册依赖
                        registerDependentBean(autowiredBeanName, beanName);
                        if (logger.isTraceEnabled()) {
                            logger.trace("Autowiring by type from bean name '" + beanName + "' via property '" + propertyName + "' to bean named '" + autowiredBeanName + "'");
                        }
                    }
                    autowiredBeanNames.clear();
                }
            } catch (BeansException ex) {
                throw new UnsatisfiedDependencyException(mbd.getResourceDescription(), beanName, propertyName, ex);
            }
        }
    }

    protected String[] unsatisfiedNonSimpleProperties(AbstractBeanDefinition mbd, BeanWrapper bw) {
        Set<String> result = new TreeSet<>();
        PropertyValues pvs = mbd.getPropertyValues();
        PropertyDescriptor[] pds = bw.getPropertyDescriptors();
        for (PropertyDescriptor pd : pds) {
            if (pd.getWriteMethod() != null && !isExcludedFromDependencyCheck(pd) && !pvs.contains(pd.getName()) && !BeanUtils.isSimpleProperty(pd.getPropertyType())) {
                result.add(pd.getName());
            }
        }
        return StringUtils.toStringArray(result);
    }

    protected PropertyDescriptor[] filterPropertyDescriptorsForDependencyCheck(BeanWrapper bw, boolean cache) {
        PropertyDescriptor[] filtered = this.filteredPropertyDescriptorsCache.get(bw.getWrappedClass());
        if (filtered == null) {
            filtered = filterPropertyDescriptorsForDependencyCheck(bw);
            if (cache) {
                PropertyDescriptor[] existing = this.filteredPropertyDescriptorsCache.putIfAbsent(bw.getWrappedClass(), filtered);
                if (existing != null) {
                    filtered = existing;
                }
            }
        }
        return filtered;
    }

    protected PropertyDescriptor[] filterPropertyDescriptorsForDependencyCheck(BeanWrapper bw) {
        List<PropertyDescriptor> pds = new ArrayList<>(Arrays.asList(bw.getPropertyDescriptors()));
        pds.removeIf(this::isExcludedFromDependencyCheck);
        return pds.toArray(new PropertyDescriptor[0]);
    }

    protected boolean isExcludedFromDependencyCheck(PropertyDescriptor pd) {
        return (AutowireUtils.isExcludedFromDependencyCheck(pd) || this.ignoredDependencyTypes.contains(pd.getPropertyType()) || AutowireUtils.isSetterDefinedInInterface(pd, this.ignoredDependencyInterfaces));
    }

    protected void checkDependencies(String beanName, AbstractBeanDefinition mbd, PropertyDescriptor[] pds, @Nullable PropertyValues pvs) throws UnsatisfiedDependencyException {
        int dependencyCheck = mbd.getDependencyCheck();
        for (PropertyDescriptor pd : pds) {
            if (pd.getWriteMethod() != null && (pvs == null || !pvs.contains(pd.getName()))) {
                boolean isSimple = BeanUtils.isSimpleProperty(pd.getPropertyType());
                boolean unsatisfied = (dependencyCheck == AbstractBeanDefinition.DEPENDENCY_CHECK_ALL) || (isSimple && dependencyCheck == AbstractBeanDefinition.DEPENDENCY_CHECK_SIMPLE) || (!isSimple && dependencyCheck == AbstractBeanDefinition.DEPENDENCY_CHECK_OBJECTS);
                if (unsatisfied) {
                    throw new UnsatisfiedDependencyException(mbd.getResourceDescription(), beanName, pd.getName(), "Set this property value or disable dependency checking for this bean.");
                }
            }
        }
    }

    protected void applyPropertyValues(String beanName, BeanDefinition mbd, BeanWrapper bw, PropertyValues pvs) {
        if (pvs.isEmpty()) {
            return;
        }
        if (System.getSecurityManager() != null && bw instanceof BeanWrapperImpl) {
            ((BeanWrapperImpl) bw).setSecurityContext(getAccessControlContext());
        }
        MutablePropertyValues mpvs = null;
        List<PropertyValue> original;
        if (pvs instanceof MutablePropertyValues) {
            mpvs = (MutablePropertyValues) pvs;
            // 如果已经converted，直接设置返回
            if (mpvs.isConverted()) {
                // Shortcut: use the pre-converted values as-is.
                try {
                    bw.setPropertyValues(mpvs);
                    return;
                } catch (BeansException ex) {
                    throw new BeanCreationException(mbd.getResourceDescription(), beanName, "Error setting property values", ex);
                }
            }
            original = mpvs.getPropertyValueList();
        } else {
            original = Arrays.asList(pvs.getPropertyValues());
        }
        TypeConverter converter = getCustomTypeConverter();
        if (converter == null) {
            converter = bw;
        }
        BeanDefinitionValueResolver valueResolver = new BeanDefinitionValueResolver(this, beanName, mbd, converter);
        // 创建一个深拷贝，解决对值的引用
        List<PropertyValue> deepCopy = new ArrayList<>(original.size());
        boolean resolveNecessary = false;
        for (PropertyValue pv : original) {
            // 已经转化了就直接添加
            if (pv.isConverted()) {
                deepCopy.add(pv);
            }
            // 没有就转化再加
            else {
                String propertyName = pv.getName();
                Object originalValue = pv.getValue();
                Object resolvedValue = valueResolver.resolveValueIfNecessary(pv, originalValue);
                Object convertedValue = resolvedValue;
                boolean convertible = bw.isWritableProperty(propertyName) && !PropertyAccessorUtils.isNestedOrIndexedProperty(propertyName);
                if (convertible) {
                    convertedValue = convertForProperty(resolvedValue, propertyName, bw, converter);
                }
                // Possibly store converted value in merged bean definition,
                // in order to avoid re-conversion for every created bean instance.
                if (resolvedValue == originalValue) {
                    if (convertible) {
                        pv.setConvertedValue(convertedValue);
                    }
                    deepCopy.add(pv);
                } else if (convertible && originalValue instanceof TypedStringValue && !((TypedStringValue) originalValue).isDynamic() && !(convertedValue instanceof Collection || ObjectUtils.isArray(convertedValue))) {
                    pv.setConvertedValue(convertedValue);
                    deepCopy.add(pv);
                } else {
                    resolveNecessary = true;
                    deepCopy.add(new PropertyValue(pv, convertedValue));
                }
            }
        }
        if (mpvs != null && !resolveNecessary) {
            mpvs.setConverted();
        }
        // Set our (possibly massaged) deep copy.
        try {
            // 调用BeanWrapper的方法注入
            bw.setPropertyValues(new MutablePropertyValues(deepCopy));
        } catch (BeansException ex) {
            throw new BeanCreationException(mbd.getResourceDescription(), beanName, "Error setting property values", ex);
        }
    }

    @Nullable
    private Object convertForProperty(@Nullable Object value, String propertyName, BeanWrapper bw, TypeConverter converter) {
        if (converter instanceof BeanWrapperImpl) {
            return ((BeanWrapperImpl) converter).convertForProperty(value, propertyName);
        } else {
            PropertyDescriptor pd = bw.getPropertyDescriptor(propertyName);
            MethodParameter methodParam = BeanUtils.getWriteMethodParameter(pd);
            return converter.convertIfNecessary(value, pd.getPropertyType(), methodParam);
        }
    }

    /**
     * 回调
     */
    protected Object initializeBean(final String beanName, final Object bean, @Nullable RootBeanDefinition mbd) {
        if (System.getSecurityManager() != null) {
            AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
                invokeAwareMethods(beanName, bean);
                return null;
            }, getAccessControlContext());
        } else {
            // 如果Bean实现了BeanNameAware、BeanClassLoaderAware或BeanFactoryAware接口，回调
            invokeAwareMethods(beanName, bean);
        }
        Object wrappedBean = bean;
        if (mbd == null || !mbd.isSynthetic()) {
            // 遍历已实例化添加的BeanPostProcessor，调用postProcessBeforeInitialization
            wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);
        }
        try {
            // 如果Bean实现了InitializingBean接口，调用afterPropertiesSet
            // 如果Bean定义了initMethod方法，反射调用initMethod
            invokeInitMethods(beanName, wrappedBean, mbd);
        } catch (Throwable ex) {
            throw new BeanCreationException((mbd != null ? mbd.getResourceDescription() : null), beanName, "Invocation of init method failed", ex);
        }
        if (mbd == null || !mbd.isSynthetic()) {
            // 遍历已实例化添加的BeanPostProcessor，调用postProcessAfterInitialization
            // AOP会在创建Bean实例最后进行代理增强
            wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
        }
        return wrappedBean;
    }

    private void invokeAwareMethods(final String beanName, final Object bean) {
        if (bean instanceof Aware) {
            if (bean instanceof BeanNameAware) {
                // 设置自己的名字
                ((BeanNameAware) bean).setBeanName(beanName);
            }
            if (bean instanceof BeanClassLoaderAware) {
                ClassLoader bcl = getBeanClassLoader();
                if (bcl != null) {
                    ((BeanClassLoaderAware) bean).setBeanClassLoader(bcl);
                }
            }
            if (bean instanceof BeanFactoryAware) {
                // 注入
                ((BeanFactoryAware) bean).setBeanFactory(AbstractAutowireCapableBeanFactory.this);
            }
        }
    }

    protected void invokeInitMethods(String beanName, final Object bean, @Nullable RootBeanDefinition mbd) throws Throwable {
        boolean isInitializingBean = (bean instanceof InitializingBean);
        if (isInitializingBean && (mbd == null || !mbd.isExternallyManagedInitMethod("afterPropertiesSet"))) {
            if (logger.isTraceEnabled()) {
                logger.trace("Invoking afterPropertiesSet() on bean with name '" + beanName + "'");
            }
            if (System.getSecurityManager() != null) {
                try {
                    AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
                        ((InitializingBean) bean).afterPropertiesSet();
                        return null;
                    }, getAccessControlContext());
                } catch (PrivilegedActionException pae) {
                    throw pae.getException();
                }
            } else {
                ((InitializingBean) bean).afterPropertiesSet();
            }
        }
        if (mbd != null && bean.getClass() != NullBean.class) {
            String initMethodName = mbd.getInitMethodName();
            if (StringUtils.hasLength(initMethodName) && !(isInitializingBean && "afterPropertiesSet".equals(initMethodName)) && !mbd.isExternallyManagedInitMethod(initMethodName)) {
                invokeCustomInitMethod(beanName, bean, mbd);
            }
        }
    }

    protected void invokeCustomInitMethod(String beanName, final Object bean, RootBeanDefinition mbd) throws Throwable {
        String initMethodName = mbd.getInitMethodName();
        Assert.state(initMethodName != null, "No init method set");
        final Method initMethod = (mbd.isNonPublicAccessAllowed() ? BeanUtils.findMethod(bean.getClass(), initMethodName) : ClassUtils.getMethodIfAvailable(bean.getClass(), initMethodName));
        if (initMethod == null) {
            if (mbd.isEnforceInitMethod()) {
                throw new BeanDefinitionValidationException("Could not find an init method named '" + initMethodName + "' on bean with name '" + beanName + "'");
            } else {
                if (logger.isTraceEnabled()) {
                    logger.trace("No default init method named '" + initMethodName + "' found on bean with name '" + beanName + "'");
                }
                // Ignore non-existent default lifecycle methods.
                return;
            }
        }
        if (logger.isTraceEnabled()) {
            logger.trace("Invoking init method  '" + initMethodName + "' on bean with name '" + beanName + "'");
        }
        if (System.getSecurityManager() != null) {
            AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
                ReflectionUtils.makeAccessible(initMethod);
                return null;
            });
            try {
                AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> initMethod.invoke(bean), getAccessControlContext());
            } catch (PrivilegedActionException pae) {
                InvocationTargetException ex = (InvocationTargetException) pae.getException();
                throw ex.getTargetException();
            }
        } else {
            try {
                ReflectionUtils.makeAccessible(initMethod);
                initMethod.invoke(bean);
            } catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            }
        }
    }

    @Override
    protected Object postProcessObjectFromFactoryBean(Object object, String beanName) {
        return applyBeanPostProcessorsAfterInitialization(object, beanName);
    }

    @Override
    protected void removeSingleton(String beanName) {
        synchronized (getSingletonMutex()) {
            super.removeSingleton(beanName);
            this.factoryBeanInstanceCache.remove(beanName);
        }
    }

    @Override
    protected void clearSingletonCache() {
        synchronized (getSingletonMutex()) {
            super.clearSingletonCache();
            this.factoryBeanInstanceCache.clear();
        }
    }

    Log getLogger() {
        return logger;
    }

    @SuppressWarnings("serial")
    private static class AutowireByTypeDependencyDescriptor extends DependencyDescriptor {

        public AutowireByTypeDependencyDescriptor(MethodParameter methodParameter, boolean eager) {
            super(methodParameter, false, eager);
        }

        @Override
        public String getDependencyName() {
            return null;
        }

    }

}
