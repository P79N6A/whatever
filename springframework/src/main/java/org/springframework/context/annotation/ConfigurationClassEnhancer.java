package org.springframework.context.annotation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.scope.ScopedProxyFactoryBean;
import org.springframework.asm.Type;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.SimpleInstantiationStrategy;
import org.springframework.cglib.core.ClassGenerator;
import org.springframework.cglib.core.Constants;
import org.springframework.cglib.core.DefaultGeneratorStrategy;
import org.springframework.cglib.core.SpringNamingPolicy;
import org.springframework.cglib.proxy.*;
import org.springframework.cglib.transform.ClassEmitterTransformer;
import org.springframework.cglib.transform.TransformingClassGenerator;
import org.springframework.lang.Nullable;
import org.springframework.objenesis.ObjenesisException;
import org.springframework.objenesis.SpringObjenesis;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;

class ConfigurationClassEnhancer {

    // The callbacks to use. Note that these callbacks must be stateless.
    private static final Callback[] CALLBACKS = new Callback[]{
            new BeanMethodInterceptor(), // 拦截方法调用
            new BeanFactoryAwareMethodInterceptor(), // 拦截BeanFactoryAware#setBeanFactory
            NoOp.INSTANCE
    };

    private static final ConditionalCallbackFilter CALLBACK_FILTER = new ConditionalCallbackFilter(CALLBACKS);

    private static final String BEAN_FACTORY_FIELD = "$$beanFactory";

    private static final Log logger = LogFactory.getLog(ConfigurationClassEnhancer.class);

    private static final SpringObjenesis objenesis = new SpringObjenesis();

    public Class<?> enhance(Class<?> configClass, @Nullable ClassLoader classLoader) {
        if (EnhancedConfiguration.class.isAssignableFrom(configClass)) {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Ignoring request to enhance %s as it has " + "already been enhanced. This usually indicates that more than one " + "ConfigurationClassPostProcessor has been registered (e.g. via " + "<context:annotation-config>). This is harmless, but you may " + "want check your configuration and remove one CCPP if possible", configClass.getName()));
            }
            return configClass;
        }
        Class<?> enhancedClass = createClass(newEnhancer(configClass, classLoader));
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("Successfully enhanced %s; enhanced class name is: %s", configClass.getName(), enhancedClass.getName()));
        }
        return enhancedClass;
    }

    /**
     * 创建一个新的CGLIB Enhancer实例，configSuperClass是要增强的配置类
     */
    private Enhancer newEnhancer(Class<?> configSuperClass, @Nullable ClassLoader classLoader) {
        Enhancer enhancer = new Enhancer();
        // 设置被增强类的父类是配置类
        enhancer.setSuperclass(configSuperClass);
        // 为增强类增加新的接口EnhancedConfiguration，主要目的是增加接口BeanFactoryAware
        enhancer.setInterfaces(new Class<?>[]{EnhancedConfiguration.class});
        enhancer.setUseFactory(false);
        enhancer.setNamingPolicy(SpringNamingPolicy.INSTANCE);
        enhancer.setStrategy(new BeanFactoryAwareGeneratorStrategy(classLoader));
        // 增强器就是在方法调用前后做拦截，CALLBACK_FILTER就相当于为被增强类增加的功能
        enhancer.setCallbackFilter(CALLBACK_FILTER);
        enhancer.setCallbackTypes(CALLBACK_FILTER.getCallbackTypes());
        return enhancer;
    }

    /**
     * 使用增强器创建一个新的类，新建类是被增强类的子类
     */
    private Class<?> createClass(Enhancer enhancer) {
        Class<?> subclass = enhancer.createClass();
        // Registering callbacks statically (as opposed to thread-local)
        // is critical for usage in an OSGi environment (SPR-5932)...
        Enhancer.registerStaticCallbacks(subclass, CALLBACKS);
        return subclass;
    }


    /**
     * 用于增强配置类，增强之后的类实现该接口
     */
    public interface EnhancedConfiguration extends BeanFactoryAware {
    }

    private interface ConditionalCallback extends Callback {

        boolean isMatch(Method candidateMethod);

    }

    private static class ConditionalCallbackFilter implements CallbackFilter {

        private final Callback[] callbacks;

        private final Class<?>[] callbackTypes;

        public ConditionalCallbackFilter(Callback[] callbacks) {
            this.callbacks = callbacks;
            this.callbackTypes = new Class<?>[callbacks.length];
            for (int i = 0; i < callbacks.length; i++) {
                this.callbackTypes[i] = callbacks[i].getClass();
            }
        }

        @Override
        public int accept(Method method) {
            for (int i = 0; i < this.callbacks.length; i++) {
                Callback callback = this.callbacks[i];
                if (!(callback instanceof ConditionalCallback) || ((ConditionalCallback) callback).isMatch(method)) {
                    return i;
                }
            }
            throw new IllegalStateException("No callback available for method " + method.getName());
        }

        public Class<?>[] getCallbackTypes() {
            return this.callbackTypes;
        }

    }

    private static class BeanFactoryAwareGeneratorStrategy extends DefaultGeneratorStrategy {

        @Nullable
        private final ClassLoader classLoader;

        public BeanFactoryAwareGeneratorStrategy(@Nullable ClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        @Override
        protected ClassGenerator transform(ClassGenerator cg) throws Exception {
            ClassEmitterTransformer transformer = new ClassEmitterTransformer() {
                @Override
                public void end_class() {
                    declare_field(Constants.ACC_PUBLIC, BEAN_FACTORY_FIELD, Type.getType(BeanFactory.class), null);
                    super.end_class();
                }
            };
            return new TransformingClassGenerator(cg, transformer);
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
                // Cannot access thread context ClassLoader - falling back...
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
                    // Reset original thread context ClassLoader.
                    currentThread.setContextClassLoader(threadContextClassLoader);
                }
            }
        }

    }

    private static class BeanFactoryAwareMethodInterceptor implements MethodInterceptor, ConditionalCallback {

        @Override
        @Nullable
        public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
            Field field = ReflectionUtils.findField(obj.getClass(), BEAN_FACTORY_FIELD);
            Assert.state(field != null, "Unable to find generated BeanFactory field");
            field.set(obj, args[0]);
            // Does the actual (non-CGLIB) superclass implement BeanFactoryAware?
            // If so, call its setBeanFactory() method. If not, just exit.
            if (BeanFactoryAware.class.isAssignableFrom(ClassUtils.getUserClass(obj.getClass().getSuperclass()))) {
                return proxy.invokeSuper(obj, args);
            }
            return null;
        }

        @Override
        public boolean isMatch(Method candidateMethod) {
            return isSetBeanFactory(candidateMethod);
        }

        public static boolean isSetBeanFactory(Method candidateMethod) {
            return (candidateMethod.getName().equals("setBeanFactory") && candidateMethod.getParameterCount() == 1 && BeanFactory.class == candidateMethod.getParameterTypes()[0] && BeanFactoryAware.class.isAssignableFrom(candidateMethod.getDeclaringClass()));
        }

    }

    private static class BeanMethodInterceptor implements MethodInterceptor, ConditionalCallback {

        @Override
        @Nullable
        public Object intercept(Object enhancedConfigInstance, Method beanMethod, Object[] beanMethodArgs, MethodProxy cglibMethodProxy) throws Throwable {
            ConfigurableBeanFactory beanFactory = getBeanFactory(enhancedConfigInstance);
            String beanName = BeanAnnotationHelper.determineBeanNameFor(beanMethod);
            // Determine whether this bean is a scoped-proxy
            if (BeanAnnotationHelper.isScopedProxy(beanMethod)) {
                String scopedBeanName = ScopedProxyCreator.getTargetBeanName(beanName);
                if (beanFactory.isCurrentlyInCreation(scopedBeanName)) {
                    beanName = scopedBeanName;
                }
            }
            // To handle the case of an inter-bean method reference, we must explicitly check the
            // container for already cached instances.
            // First, check to see if the requested bean is a FactoryBean. If so, create a subclass
            // proxy that intercepts calls to getObject() and returns any cached bean instance.
            // This ensures that the semantics of calling a FactoryBean from within @Bean methods
            // is the same as that of referring to a FactoryBean within XML. See SPR-6602.
            if (factoryContainsBean(beanFactory, BeanFactory.FACTORY_BEAN_PREFIX + beanName) && factoryContainsBean(beanFactory, beanName)) {
                Object factoryBean = beanFactory.getBean(BeanFactory.FACTORY_BEAN_PREFIX + beanName);
                if (factoryBean instanceof ScopedProxyFactoryBean) {
                    // Scoped proxy factory beans are a special case and should not be further proxied
                } else {
                    // It is a candidate FactoryBean - go ahead with enhancement
                    return enhanceFactoryBean(factoryBean, beanMethod.getReturnType(), beanFactory, beanName);
                }
            }
            if (isCurrentlyInvokedFactoryMethod(beanMethod)) {
                // The factory is calling the bean method in order to instantiate and register the bean
                // (i.e. via a getBean() call) -> invoke the super implementation of the method to actually
                // create the bean instance.
                if (logger.isInfoEnabled() && BeanFactoryPostProcessor.class.isAssignableFrom(beanMethod.getReturnType())) {
                    logger.info(String.format("@Bean method %s.%s is non-static and returns an object " + "assignable to Spring's BeanFactoryPostProcessor interface. This will " + "result in a failure to process annotations such as @Autowired, " + "@Resource and @PostConstruct within the method's declaring " + "@Configuration class. Add the 'static' modifier to this method to avoid " + "these container lifecycle issues; see @Bean javadoc for complete details.", beanMethod.getDeclaringClass().getSimpleName(), beanMethod.getName()));
                }
                return cglibMethodProxy.invokeSuper(enhancedConfigInstance, beanMethodArgs);
            }
            return resolveBeanReference(beanMethod, beanMethodArgs, beanFactory, beanName);
        }

        private Object resolveBeanReference(Method beanMethod, Object[] beanMethodArgs, ConfigurableBeanFactory beanFactory, String beanName) {
            // The user (i.e. not the factory) is requesting this bean through a call to
            // the bean method, direct or indirect. The bean may have already been marked
            // as 'in creation' in certain autowiring scenarios; if so, temporarily set
            // the in-creation status to false in order to avoid an exception.
            boolean alreadyInCreation = beanFactory.isCurrentlyInCreation(beanName);
            try {
                if (alreadyInCreation) {
                    beanFactory.setCurrentlyInCreation(beanName, false);
                }
                boolean useArgs = !ObjectUtils.isEmpty(beanMethodArgs);
                if (useArgs && beanFactory.isSingleton(beanName)) {
                    // Stubbed null arguments just for reference purposes,
                    // expecting them to be autowired for regular singleton references?
                    // A safe assumption since @Bean singleton arguments cannot be optional...
                    for (Object arg : beanMethodArgs) {
                        if (arg == null) {
                            useArgs = false;
                            break;
                        }
                    }
                }
                Object beanInstance = (useArgs ? beanFactory.getBean(beanName, beanMethodArgs) : beanFactory.getBean(beanName));
                if (!ClassUtils.isAssignableValue(beanMethod.getReturnType(), beanInstance)) {
                    // Detect package-protected NullBean instance through equals(null) check
                    if (beanInstance.equals(null)) {
                        if (logger.isDebugEnabled()) {
                            logger.debug(String.format("@Bean method %s.%s called as bean reference " + "for type [%s] returned null bean; resolving to null value.", beanMethod.getDeclaringClass().getSimpleName(), beanMethod.getName(), beanMethod.getReturnType().getName()));
                        }
                        beanInstance = null;
                    } else {
                        String msg = String.format("@Bean method %s.%s called as bean reference " + "for type [%s] but overridden by non-compatible bean instance of type [%s].", beanMethod.getDeclaringClass().getSimpleName(), beanMethod.getName(), beanMethod.getReturnType().getName(), beanInstance.getClass().getName());
                        try {
                            BeanDefinition beanDefinition = beanFactory.getMergedBeanDefinition(beanName);
                            msg += " Overriding bean of same name declared in: " + beanDefinition.getResourceDescription();
                        } catch (NoSuchBeanDefinitionException ex) {
                            // Ignore - simply no detailed message then.
                        }
                        throw new IllegalStateException(msg);
                    }
                }
                Method currentlyInvoked = SimpleInstantiationStrategy.getCurrentlyInvokedFactoryMethod();
                if (currentlyInvoked != null) {
                    String outerBeanName = BeanAnnotationHelper.determineBeanNameFor(currentlyInvoked);
                    beanFactory.registerDependentBean(beanName, outerBeanName);
                }
                return beanInstance;
            } finally {
                if (alreadyInCreation) {
                    beanFactory.setCurrentlyInCreation(beanName, true);
                }
            }
        }

        @Override
        public boolean isMatch(Method candidateMethod) {
            return (candidateMethod.getDeclaringClass() != Object.class && !BeanFactoryAwareMethodInterceptor.isSetBeanFactory(candidateMethod) && BeanAnnotationHelper.isBeanAnnotated(candidateMethod));
        }

        private ConfigurableBeanFactory getBeanFactory(Object enhancedConfigInstance) {
            Field field = ReflectionUtils.findField(enhancedConfigInstance.getClass(), BEAN_FACTORY_FIELD);
            Assert.state(field != null, "Unable to find generated bean factory field");
            Object beanFactory = ReflectionUtils.getField(field, enhancedConfigInstance);
            Assert.state(beanFactory != null, "BeanFactory has not been injected into @Configuration class");
            Assert.state(beanFactory instanceof ConfigurableBeanFactory, "Injected BeanFactory is not a ConfigurableBeanFactory");
            return (ConfigurableBeanFactory) beanFactory;
        }

        private boolean factoryContainsBean(ConfigurableBeanFactory beanFactory, String beanName) {
            return (beanFactory.containsBean(beanName) && !beanFactory.isCurrentlyInCreation(beanName));
        }

        private boolean isCurrentlyInvokedFactoryMethod(Method method) {
            Method currentlyInvoked = SimpleInstantiationStrategy.getCurrentlyInvokedFactoryMethod();
            return (currentlyInvoked != null && method.getName().equals(currentlyInvoked.getName()) && Arrays.equals(method.getParameterTypes(), currentlyInvoked.getParameterTypes()));
        }

        private Object enhanceFactoryBean(final Object factoryBean, Class<?> exposedType, final ConfigurableBeanFactory beanFactory, final String beanName) {
            try {
                Class<?> clazz = factoryBean.getClass();
                boolean finalClass = Modifier.isFinal(clazz.getModifiers());
                boolean finalMethod = Modifier.isFinal(clazz.getMethod("getObject").getModifiers());
                if (finalClass || finalMethod) {
                    if (exposedType.isInterface()) {
                        if (logger.isTraceEnabled()) {
                            logger.trace("Creating interface proxy for FactoryBean '" + beanName + "' of type [" + clazz.getName() + "] for use within another @Bean method because its " + (finalClass ? "implementation class" : "getObject() method") + " is final: Otherwise a getObject() call would not be routed to the factory.");
                        }
                        return createInterfaceProxyForFactoryBean(factoryBean, exposedType, beanFactory, beanName);
                    } else {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Unable to proxy FactoryBean '" + beanName + "' of type [" + clazz.getName() + "] for use within another @Bean method because its " + (finalClass ? "implementation class" : "getObject() method") + " is final: A getObject() call will NOT be routed to the factory. " + "Consider declaring the return type as a FactoryBean interface.");
                        }
                        return factoryBean;
                    }
                }
            } catch (NoSuchMethodException ex) {
                // No getObject() method -> shouldn't happen, but as long as nobody is trying to call it...
            }
            return createCglibProxyForFactoryBean(factoryBean, beanFactory, beanName);
        }

        private Object createInterfaceProxyForFactoryBean(final Object factoryBean, Class<?> interfaceType, final ConfigurableBeanFactory beanFactory, final String beanName) {
            return Proxy.newProxyInstance(factoryBean.getClass().getClassLoader(), new Class<?>[]{interfaceType}, (proxy, method, args) -> {
                if (method.getName().equals("getObject") && args == null) {
                    return beanFactory.getBean(beanName);
                }
                return ReflectionUtils.invokeMethod(method, factoryBean, args);
            });
        }

        private Object createCglibProxyForFactoryBean(final Object factoryBean, final ConfigurableBeanFactory beanFactory, final String beanName) {
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(factoryBean.getClass());
            enhancer.setNamingPolicy(SpringNamingPolicy.INSTANCE);
            enhancer.setCallbackType(MethodInterceptor.class);
            // Ideally create enhanced FactoryBean proxy without constructor side effects,
            // analogous to AOP proxy creation in ObjenesisCglibAopProxy...
            Class<?> fbClass = enhancer.createClass();
            Object fbProxy = null;
            if (objenesis.isWorthTrying()) {
                try {
                    fbProxy = objenesis.newInstance(fbClass, enhancer.getUseCache());
                } catch (ObjenesisException ex) {
                    logger.debug("Unable to instantiate enhanced FactoryBean using Objenesis, " + "falling back to regular construction", ex);
                }
            }
            if (fbProxy == null) {
                try {
                    fbProxy = ReflectionUtils.accessibleConstructor(fbClass).newInstance();
                } catch (Throwable ex) {
                    throw new IllegalStateException("Unable to instantiate enhanced FactoryBean using Objenesis, " + "and regular FactoryBean instantiation via default constructor fails as well", ex);
                }
            }
            ((Factory) fbProxy).setCallback(0, (MethodInterceptor) (obj, method, args, proxy) -> {
                if (method.getName().equals("getObject") && args.length == 0) {
                    return beanFactory.getBean(beanName);
                }
                return proxy.invoke(factoryBean, args);
            });
            return fbProxy;
        }

    }

}
