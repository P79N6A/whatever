package org.springframework.aop.framework;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.*;
import org.springframework.aop.support.AopUtils;
import org.springframework.cglib.core.ClassGenerator;
import org.springframework.cglib.core.CodeGenerationException;
import org.springframework.cglib.core.SpringNamingPolicy;
import org.springframework.cglib.proxy.*;
import org.springframework.cglib.transform.impl.UndeclaredThrowableStrategy;
import org.springframework.core.SmartClassLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.*;

@SuppressWarnings("serial")
class CglibAopProxy implements AopProxy, Serializable {

    private static final int AOP_PROXY = 0;

    private static final int INVOKE_TARGET = 1;

    private static final int NO_OVERRIDE = 2;

    private static final int DISPATCH_TARGET = 3;

    private static final int DISPATCH_ADVISED = 4;

    private static final int INVOKE_EQUALS = 5;

    private static final int INVOKE_HASHCODE = 6;

    protected static final Log logger = LogFactory.getLog(CglibAopProxy.class);

    private static final Map<Class<?>, Boolean> validatedClasses = new WeakHashMap<>();

    protected final AdvisedSupport advised;

    @Nullable
    protected Object[] constructorArgs;

    @Nullable
    protected Class<?>[] constructorArgTypes;

    private final transient AdvisedDispatcher advisedDispatcher;

    private transient Map<Method, Integer> fixedInterceptorMap = Collections.emptyMap();

    private transient int fixedInterceptorOffset;

    public CglibAopProxy(AdvisedSupport config) throws AopConfigException {
        Assert.notNull(config, "AdvisedSupport must not be null");
        if (config.getAdvisors().length == 0 && config.getTargetSource() == AdvisedSupport.EMPTY_TARGET_SOURCE) {
            throw new AopConfigException("No advisors and no TargetSource specified");
        }
        this.advised = config;
        this.advisedDispatcher = new AdvisedDispatcher(this.advised);
    }

    public void setConstructorArguments(@Nullable Object[] constructorArgs, @Nullable Class<?>[] constructorArgTypes) {
        if (constructorArgs == null || constructorArgTypes == null) {
            throw new IllegalArgumentException("Both 'constructorArgs' and 'constructorArgTypes' need to be specified");
        }
        if (constructorArgs.length != constructorArgTypes.length) {
            throw new IllegalArgumentException("Number of 'constructorArgs' (" + constructorArgs.length + ") must match number of 'constructorArgTypes' (" + constructorArgTypes.length + ")");
        }
        this.constructorArgs = constructorArgs;
        this.constructorArgTypes = constructorArgTypes;
    }

    @Override
    public Object getProxy() {
        return getProxy(null);
    }

    @Override
    public Object getProxy(@Nullable ClassLoader classLoader) {
        if (logger.isTraceEnabled()) {
            logger.trace("Creating CGLIB proxy: " + this.advised.getTargetSource());
        }
        try {
            // 目标类
            Class<?> rootClass = this.advised.getTargetClass();
            Assert.state(rootClass != null, "Target class must be available for creating a CGLIB proxy");
            Class<?> proxySuperClass = rootClass;
            // 包含$$，是生成的代理
            if (rootClass.getName().contains(ClassUtils.CGLIB_CLASS_SEPARATOR)) {
                // 父类
                proxySuperClass = rootClass.getSuperclass();
                Class<?>[] additionalInterfaces = rootClass.getInterfaces();
                for (Class<?> additionalInterface : additionalInterfaces) {
                    this.advised.addInterface(additionalInterface);
                }
            }
            validateClassIfNecessary(proxySuperClass, classLoader);
            // Enhancer
            Enhancer enhancer = createEnhancer();
            if (classLoader != null) {
                enhancer.setClassLoader(classLoader);
                if (classLoader instanceof SmartClassLoader && ((SmartClassLoader) classLoader).isClassReloadable(proxySuperClass)) {
                    enhancer.setUseCache(false);
                }
            }
            enhancer.setSuperclass(proxySuperClass);
            enhancer.setInterfaces(AopProxyUtils.completeProxiedInterfaces(this.advised));
            // 命名策略，加$$
            enhancer.setNamingPolicy(SpringNamingPolicy.INSTANCE);
            // 类生成策略，直接生成字节码
            enhancer.setStrategy(new ClassLoaderAwareUndeclaredThrowableStrategy(classLoader));
            // 获取所有Callback，把Advisor封装成Callback
            Callback[] callbacks = getCallbacks(rootClass);
            // 存放
            Class<?>[] types = new Class<?>[callbacks.length];
            // 生成Callback的类数组
            for (int x = 0; x < types.length; x++) {
                types[x] = callbacks[x].getClass();
            }
            /*
             * Callback过滤器，根据Filter返回值，执行不同Callback
             * 0:AOP_PROXY
             * 1:INVOKE_TARGET
             * 2:NO_OVERRIDE
             * 3:DISPATCH_TARGET
             * 4:DISPATCH_ADVISED
             * 5:INVOKE_EQUALS
             * 6:INVOKE_HASHCODE
             */
            enhancer.setCallbackFilter(new ProxyCallbackFilter(this.advised.getConfigurationOnlyCopy(), this.fixedInterceptorMap, this.fixedInterceptorOffset));
            enhancer.setCallbackTypes(types);
            return createProxyClassAndInstance(enhancer, callbacks);
        } catch (CodeGenerationException | IllegalArgumentException ex) {
            throw new AopConfigException("Could not generate CGLIB subclass of " + this.advised.getTargetClass() + ": Common causes of this problem include using a final class or a non-visible class", ex);
        } catch (Throwable ex) {
            throw new AopConfigException("Unexpected AOP exception", ex);
        }
    }

    protected Object createProxyClassAndInstance(Enhancer enhancer, Callback[] callbacks) {
        // 不拦截构造方法
        enhancer.setInterceptDuringConstruction(false);
        // 拦截器回调
        enhancer.setCallbacks(callbacks);
        // 创建代理实例
        return (this.constructorArgs != null && this.constructorArgTypes != null ? enhancer.create(this.constructorArgTypes, this.constructorArgs) : enhancer.create());
    }

    protected Enhancer createEnhancer() {
        return new Enhancer();
    }

    private void validateClassIfNecessary(Class<?> proxySuperClass, @Nullable ClassLoader proxyClassLoader) {
        if (logger.isWarnEnabled()) {
            synchronized (validatedClasses) {
                if (!validatedClasses.containsKey(proxySuperClass)) {
                    doValidateClass(proxySuperClass, proxyClassLoader, ClassUtils.getAllInterfacesForClassAsSet(proxySuperClass));
                    validatedClasses.put(proxySuperClass, Boolean.TRUE);
                }
            }
        }
    }

    private void doValidateClass(Class<?> proxySuperClass, @Nullable ClassLoader proxyClassLoader, Set<Class<?>> ifcs) {
        if (proxySuperClass != Object.class) {
            Method[] methods = proxySuperClass.getDeclaredMethods();
            for (Method method : methods) {
                int mod = method.getModifiers();
                if (!Modifier.isStatic(mod) && !Modifier.isPrivate(mod)) {
                    if (Modifier.isFinal(mod)) {
                        if (implementsInterface(method, ifcs)) {
                            logger.info("Unable to proxy interface-implementing method [" + method + "] because " + "it is marked as final: Consider using interface-based JDK proxies instead!");
                        }
                        logger.debug("Final method [" + method + "] cannot get proxied via CGLIB: " + "Calls to this method will NOT be routed to the target instance and " + "might lead to NPEs against uninitialized fields in the proxy instance.");
                    } else if (!Modifier.isPublic(mod) && !Modifier.isProtected(mod) && proxyClassLoader != null && proxySuperClass.getClassLoader() != proxyClassLoader) {
                        logger.debug("Method [" + method + "] is package-visible across different ClassLoaders " + "and cannot get proxied via CGLIB: Declare this method as public or protected " + "if you need to support invocations through the proxy.");
                    }
                }
            }
            doValidateClass(proxySuperClass.getSuperclass(), proxyClassLoader, ifcs);
        }
    }

    private Callback[] getCallbacks(Class<?> rootClass) throws Exception {
        // 是否暴露代理对象到AopProxyContext
        boolean exposeProxy = this.advised.isExposeProxy();
        boolean isFrozen = this.advised.isFrozen();
        boolean isStatic = this.advised.getTargetSource().isStatic();
        // AopInterceptor，用于AOP调用
        Callback aopInterceptor = new DynamicAdvisedInterceptor(this.advised);
        Callback targetInterceptor;
        if (exposeProxy) {
            // isStatic==true则targetSource的target是静态的，直接缓存，false表示是动态的，每次都要getTarget获取
            targetInterceptor = (isStatic ? new StaticUnadvisedExposedInterceptor(this.advised.getTargetSource().getTarget()) : new DynamicUnadvisedExposedInterceptor(this.advised.getTargetSource()));
        } else {
            targetInterceptor = (isStatic ? new StaticUnadvisedInterceptor(this.advised.getTargetSource().getTarget()) : new DynamicUnadvisedInterceptor(this.advised.getTargetSource()));
        }
        Callback targetDispatcher = (isStatic ? new StaticDispatcher(this.advised.getTargetSource().getTarget()) : new SerializableNoOp());
        Callback[] mainCallbacks = new Callback[]{aopInterceptor, targetInterceptor, new SerializableNoOp(), targetDispatcher, this.advisedDispatcher,
                // equals
                new EqualsInterceptor(this.advised),
                // hashCode
                new HashCodeInterceptor(this.advised)};
        Callback[] callbacks;
        if (isStatic && isFrozen) {
            Method[] methods = rootClass.getMethods();
            Callback[] fixedCallbacks = new Callback[methods.length];
            this.fixedInterceptorMap = new HashMap<>(methods.length);
            for (int x = 0; x < methods.length; x++) {
                Method method = methods[x];
                // 拦截器链
                List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, rootClass);
                fixedCallbacks[x] = new FixedChainStaticTargetInterceptor(chain, this.advised.getTargetSource().getTarget(), this.advised.getTargetClass());
                // fixedInterceptorMap与callbackFilter关联
                this.fixedInterceptorMap.put(method, x);
            }
            // 合并Callback
            callbacks = new Callback[mainCallbacks.length + fixedCallbacks.length];
            System.arraycopy(mainCallbacks, 0, callbacks, 0, mainCallbacks.length);
            System.arraycopy(fixedCallbacks, 0, callbacks, mainCallbacks.length, fixedCallbacks.length);
            // 标记fixedInterceptor的偏移量
            this.fixedInterceptorOffset = mainCallbacks.length;
        } else {
            callbacks = mainCallbacks;
        }
        return callbacks;
    }

    @Override
    public boolean equals(Object other) {
        return (this == other || (other instanceof CglibAopProxy && AopProxyUtils.equalsInProxy(this.advised, ((CglibAopProxy) other).advised)));
    }

    @Override
    public int hashCode() {
        return CglibAopProxy.class.hashCode() * 13 + this.advised.getTargetSource().hashCode();
    }

    private static boolean implementsInterface(Method method, Set<Class<?>> ifcs) {
        for (Class<?> ifc : ifcs) {
            if (ClassUtils.hasMethod(ifc, method.getName(), method.getParameterTypes())) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static Object processReturnType(Object proxy, @Nullable Object target, Method method, @Nullable Object returnValue) {
        if (returnValue != null && returnValue == target && !RawTargetAccess.class.isAssignableFrom(method.getDeclaringClass())) {
            returnValue = proxy;
        }
        Class<?> returnType = method.getReturnType();
        if (returnValue == null && returnType != Void.TYPE && returnType.isPrimitive()) {
            throw new AopInvocationException("Null return value from advice does not match primitive return type for: " + method);
        }
        return returnValue;
    }

    public static class SerializableNoOp implements NoOp, Serializable {
    }

    private static class StaticUnadvisedInterceptor implements MethodInterceptor, Serializable {

        @Nullable
        private final Object target;

        public StaticUnadvisedInterceptor(@Nullable Object target) {
            this.target = target;
        }

        @Override
        @Nullable
        public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
            Object retVal = methodProxy.invoke(this.target, args);
            return processReturnType(proxy, this.target, method, retVal);
        }

    }

    private static class StaticUnadvisedExposedInterceptor implements MethodInterceptor, Serializable {

        @Nullable
        private final Object target;

        public StaticUnadvisedExposedInterceptor(@Nullable Object target) {
            this.target = target;
        }

        @Override
        @Nullable
        public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
            Object oldProxy = null;
            try {
                oldProxy = AopContext.setCurrentProxy(proxy);
                Object retVal = methodProxy.invoke(this.target, args);
                return processReturnType(proxy, this.target, method, retVal);
            } finally {
                AopContext.setCurrentProxy(oldProxy);
            }
        }

    }

    private static class DynamicUnadvisedInterceptor implements MethodInterceptor, Serializable {

        private final TargetSource targetSource;

        public DynamicUnadvisedInterceptor(TargetSource targetSource) {
            this.targetSource = targetSource;
        }

        @Override
        @Nullable
        public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
            Object target = this.targetSource.getTarget();
            try {
                Object retVal = methodProxy.invoke(target, args);
                return processReturnType(proxy, target, method, retVal);
            } finally {
                if (target != null) {
                    this.targetSource.releaseTarget(target);
                }
            }
        }

    }

    private static class DynamicUnadvisedExposedInterceptor implements MethodInterceptor, Serializable {

        private final TargetSource targetSource;

        public DynamicUnadvisedExposedInterceptor(TargetSource targetSource) {
            this.targetSource = targetSource;
        }

        @Override
        @Nullable
        public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
            Object oldProxy = null;
            Object target = this.targetSource.getTarget();
            try {
                oldProxy = AopContext.setCurrentProxy(proxy);
                Object retVal = methodProxy.invoke(target, args);
                return processReturnType(proxy, target, method, retVal);
            } finally {
                AopContext.setCurrentProxy(oldProxy);
                if (target != null) {
                    this.targetSource.releaseTarget(target);
                }
            }
        }

    }

    private static class StaticDispatcher implements Dispatcher, Serializable {

        @Nullable
        private Object target;

        public StaticDispatcher(@Nullable Object target) {
            this.target = target;
        }

        @Override
        @Nullable
        public Object loadObject() {
            return this.target;
        }

    }

    private static class AdvisedDispatcher implements Dispatcher, Serializable {

        private final AdvisedSupport advised;

        public AdvisedDispatcher(AdvisedSupport advised) {
            this.advised = advised;
        }

        @Override
        public Object loadObject() throws Exception {
            return this.advised;
        }

    }

    private static class EqualsInterceptor implements MethodInterceptor, Serializable {

        private final AdvisedSupport advised;

        public EqualsInterceptor(AdvisedSupport advised) {
            this.advised = advised;
        }

        @Override
        public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) {
            Object other = args[0];
            if (proxy == other) {
                return true;
            }
            if (other instanceof Factory) {
                Callback callback = ((Factory) other).getCallback(INVOKE_EQUALS);
                if (!(callback instanceof EqualsInterceptor)) {
                    return false;
                }
                AdvisedSupport otherAdvised = ((EqualsInterceptor) callback).advised;
                return AopProxyUtils.equalsInProxy(this.advised, otherAdvised);
            } else {
                return false;
            }
        }

    }

    private static class HashCodeInterceptor implements MethodInterceptor, Serializable {

        private final AdvisedSupport advised;

        public HashCodeInterceptor(AdvisedSupport advised) {
            this.advised = advised;
        }

        @Override
        public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) {
            return CglibAopProxy.class.hashCode() * 13 + this.advised.getTargetSource().hashCode();
        }

    }

    private static class FixedChainStaticTargetInterceptor implements MethodInterceptor, Serializable {

        private final List<Object> adviceChain;

        @Nullable
        private final Object target;

        @Nullable
        private final Class<?> targetClass;

        public FixedChainStaticTargetInterceptor(List<Object> adviceChain, @Nullable Object target, @Nullable Class<?> targetClass) {
            this.adviceChain = adviceChain;
            this.target = target;
            this.targetClass = targetClass;
        }

        @Override
        @Nullable
        public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
            // 生成CglibMethodInvocation
            MethodInvocation invocation = new CglibMethodInvocation(proxy, this.target, method, args, this.targetClass, this.adviceChain, methodProxy);
            Object retVal = invocation.proceed();
            retVal = processReturnType(proxy, this.target, method, retVal);
            return retVal;
        }

    }

    private static class DynamicAdvisedInterceptor implements MethodInterceptor, Serializable {

        private final AdvisedSupport advised;

        public DynamicAdvisedInterceptor(AdvisedSupport advised) {
            this.advised = advised;
        }

        @Override
        @Nullable
        public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
            Object oldProxy = null;
            boolean setProxyContext = false;
            Object target = null;
            TargetSource targetSource = this.advised.getTargetSource();
            try {
                // 暴露
                if (this.advised.exposeProxy) {
                    oldProxy = AopContext.setCurrentProxy(proxy);
                    setProxyContext = true;
                }
                target = targetSource.getTarget();
                Class<?> targetClass = (target != null ? target.getClass() : null);
                // 拦截链
                List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);
                Object retVal;
                if (chain.isEmpty() && Modifier.isPublic(method.getModifiers())) {
                    Object[] argsToUse = AopProxyUtils.adaptArgumentsIfNecessary(method, args);
                    // 链是空且是public方法，直接调用
                    retVal = methodProxy.invoke(target, argsToUse);
                } else {
                    // 否则创建一个CglibMethodInvocation继续
                    retVal = new CglibMethodInvocation(proxy, target, method, args, targetClass, chain, methodProxy).proceed();
                }
                // 处理返回值
                retVal = processReturnType(proxy, target, method, retVal);
                return retVal;
            } finally {
                if (target != null && !targetSource.isStatic()) {
                    targetSource.releaseTarget(target);
                }
                if (setProxyContext) {
                    AopContext.setCurrentProxy(oldProxy);
                }
            }
        }

        @Override
        public boolean equals(Object other) {
            return (this == other || (other instanceof DynamicAdvisedInterceptor && this.advised.equals(((DynamicAdvisedInterceptor) other).advised)));
        }

        @Override
        public int hashCode() {
            return this.advised.hashCode();
        }

    }

    private static class CglibMethodInvocation extends ReflectiveMethodInvocation {

        @Nullable
        private final MethodProxy methodProxy;

        public CglibMethodInvocation(Object proxy, @Nullable Object target, Method method, Object[] arguments, @Nullable Class<?> targetClass, List<Object> interceptorsAndDynamicMethodMatchers, MethodProxy methodProxy) {
            super(proxy, target, method, arguments, targetClass, interceptorsAndDynamicMethodMatchers);
            this.methodProxy = (Modifier.isPublic(method.getModifiers()) && method.getDeclaringClass() != Object.class && !AopUtils.isEqualsMethod(method) && !AopUtils.isHashCodeMethod(method) && !AopUtils.isToStringMethod(method) ? methodProxy : null);
        }

        @Override
        protected Object invokeJoinpoint() throws Throwable {
            if (this.methodProxy != null) {
                return this.methodProxy.invoke(this.target, this.arguments);
            } else {
                return super.invokeJoinpoint();
            }
        }

    }

    private static class ProxyCallbackFilter implements CallbackFilter {

        private final AdvisedSupport advised;

        private final Map<Method, Integer> fixedInterceptorMap;

        private final int fixedInterceptorOffset;

        public ProxyCallbackFilter(AdvisedSupport advised, Map<Method, Integer> fixedInterceptorMap, int fixedInterceptorOffset) {
            this.advised = advised;
            this.fixedInterceptorMap = fixedInterceptorMap;
            this.fixedInterceptorOffset = fixedInterceptorOffset;
        }

        @Override
        public int accept(Method method) {
            // 如果是final方法，则返回NO_OVERRIDE
            if (AopUtils.isFinalizeMethod(method)) {
                logger.trace("Found finalize() method - using NO_OVERRIDE");
                return NO_OVERRIDE;
            }
            if (!this.advised.isOpaque() && method.getDeclaringClass().isInterface() && method.getDeclaringClass().isAssignableFrom(Advised.class)) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Method is declared on Advised interface: " + method);
                }
                // advised上的方法，直接调用advised对象的对应方法
                return DISPATCH_ADVISED;
            }
            // 返回调用equals
            if (AopUtils.isEqualsMethod(method)) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Found 'equals' method: " + method);
                }
                return INVOKE_EQUALS;
            }
            // 返回调用hashCode
            if (AopUtils.isHashCodeMethod(method)) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Found 'hashCode' method: " + method);
                }
                return INVOKE_HASHCODE;
            }
            Class<?> targetClass = this.advised.getTargetClass();
            List<?> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);
            // 是否有拦截器链
            boolean haveAdvice = !chain.isEmpty();
            boolean exposeProxy = this.advised.isExposeProxy();
            boolean isStatic = this.advised.getTargetSource().isStatic();
            boolean isFrozen = this.advised.isFrozen();
            if (haveAdvice || !isFrozen) {
                if (exposeProxy) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Must expose proxy on advised method: " + method);
                    }
                    // 如果需要暴露Proxy则返回代理
                    return AOP_PROXY;
                }
                Method key = method;
                if (isStatic && isFrozen && this.fixedInterceptorMap.containsKey(key)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Method has advice and optimizations are enabled: " + method);
                    }
                    int index = this.fixedInterceptorMap.get(key);
                    // 返回Callback索引
                    return (index + this.fixedInterceptorOffset);
                } else {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Unable to apply any optimizations to advised method: " + method);
                    }
                    return AOP_PROXY;
                }
            } else {
                if (exposeProxy || !isStatic) {
                    // targetInterceptor
                    return INVOKE_TARGET;
                }
                Class<?> returnType = method.getReturnType();
                // 如果返回类型是被代理类型的父类或者接口，有可能是返回this引用，需要用INVOKE_TARGET对返回值做处理
                if (targetClass != null && returnType.isAssignableFrom(targetClass)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Method return type is assignable from target type and " + "may therefore return 'this' - using INVOKE_TARGET: " + method);
                    }
                    return INVOKE_TARGET;
                } else {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Method return type ensures 'this' cannot be returned - " + "using DISPATCH_TARGET: " + method);
                    }
                    // 不需要拦截，直接返回目标调用
                    return DISPATCH_TARGET;
                }
            }
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof ProxyCallbackFilter)) {
                return false;
            }
            ProxyCallbackFilter otherCallbackFilter = (ProxyCallbackFilter) other;
            AdvisedSupport otherAdvised = otherCallbackFilter.advised;
            if (this.advised.isFrozen() != otherAdvised.isFrozen()) {
                return false;
            }
            if (this.advised.isExposeProxy() != otherAdvised.isExposeProxy()) {
                return false;
            }
            if (this.advised.getTargetSource().isStatic() != otherAdvised.getTargetSource().isStatic()) {
                return false;
            }
            if (!AopProxyUtils.equalsProxiedInterfaces(this.advised, otherAdvised)) {
                return false;
            }
            Advisor[] thisAdvisors = this.advised.getAdvisors();
            Advisor[] thatAdvisors = otherAdvised.getAdvisors();
            if (thisAdvisors.length != thatAdvisors.length) {
                return false;
            }
            for (int i = 0; i < thisAdvisors.length; i++) {
                Advisor thisAdvisor = thisAdvisors[i];
                Advisor thatAdvisor = thatAdvisors[i];
                if (!equalsAdviceClasses(thisAdvisor, thatAdvisor)) {
                    return false;
                }
                if (!equalsPointcuts(thisAdvisor, thatAdvisor)) {
                    return false;
                }
            }
            return true;
        }

        private boolean equalsAdviceClasses(Advisor a, Advisor b) {
            return (a.getAdvice().getClass() == b.getAdvice().getClass());
        }

        private boolean equalsPointcuts(Advisor a, Advisor b) {
            return (!(a instanceof PointcutAdvisor) || (b instanceof PointcutAdvisor && ObjectUtils.nullSafeEquals(((PointcutAdvisor) a).getPointcut(), ((PointcutAdvisor) b).getPointcut())));
        }

        @Override
        public int hashCode() {
            int hashCode = 0;
            Advisor[] advisors = this.advised.getAdvisors();
            for (Advisor advisor : advisors) {
                Advice advice = advisor.getAdvice();
                hashCode = 13 * hashCode + advice.getClass().hashCode();
            }
            hashCode = 13 * hashCode + (this.advised.isFrozen() ? 1 : 0);
            hashCode = 13 * hashCode + (this.advised.isExposeProxy() ? 1 : 0);
            hashCode = 13 * hashCode + (this.advised.isOptimize() ? 1 : 0);
            hashCode = 13 * hashCode + (this.advised.isOpaque() ? 1 : 0);
            return hashCode;
        }

    }

    private static class ClassLoaderAwareUndeclaredThrowableStrategy extends UndeclaredThrowableStrategy {

        @Nullable
        private final ClassLoader classLoader;

        public ClassLoaderAwareUndeclaredThrowableStrategy(@Nullable ClassLoader classLoader) {
            super(UndeclaredThrowableException.class);
            this.classLoader = classLoader;
        }

        @Override
        public byte[] generate(ClassGenerator cg) throws Exception {
            if (this.classLoader == null) {
                return super.generate(cg);
            }
            Thread currentThread = Thread.currentThread();
            ClassLoader threadContextClassLoader;
            try {
                threadContextClassLoader = currentThread.getContextClassLoader();
            } catch (Throwable ex) {
                return super.generate(cg);
            }
            boolean overrideClassLoader = !this.classLoader.equals(threadContextClassLoader);
            if (overrideClassLoader) {
                currentThread.setContextClassLoader(this.classLoader);
            }
            try {
                return super.generate(cg);
            } finally {
                if (overrideClassLoader) {
                    currentThread.setContextClassLoader(threadContextClassLoader);
                }
            }
        }

    }

}
