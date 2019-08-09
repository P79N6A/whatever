package org.springframework.aop.framework;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.Interceptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.Advisor;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.UnknownAdviceTypeException;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.*;

@SuppressWarnings("serial")
public class ProxyFactoryBean extends ProxyCreatorSupport implements FactoryBean<Object>, BeanClassLoaderAware, BeanFactoryAware {

    public static final String GLOBAL_SUFFIX = "*";

    protected final Log logger = LogFactory.getLog(getClass());

    @Nullable
    private String[] interceptorNames;

    @Nullable
    private String targetName;

    private boolean autodetectInterfaces = true;

    private boolean singleton = true;

    private AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

    private boolean freezeProxy = false;

    @Nullable
    private transient ClassLoader proxyClassLoader = ClassUtils.getDefaultClassLoader();

    private transient boolean classLoaderConfigured = false;

    @Nullable
    private transient BeanFactory beanFactory;

    private boolean advisorChainInitialized = false;

    @Nullable
    private Object singletonInstance;

    public void setProxyInterfaces(Class<?>[] proxyInterfaces) throws ClassNotFoundException {
        setInterfaces(proxyInterfaces);
    }

    public void setInterceptorNames(String... interceptorNames) {
        this.interceptorNames = interceptorNames;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public void setAutodetectInterfaces(boolean autodetectInterfaces) {
        this.autodetectInterfaces = autodetectInterfaces;
    }

    public void setSingleton(boolean singleton) {
        this.singleton = singleton;
    }

    public void setAdvisorAdapterRegistry(AdvisorAdapterRegistry advisorAdapterRegistry) {
        this.advisorAdapterRegistry = advisorAdapterRegistry;
    }

    @Override
    public void setFrozen(boolean frozen) {
        this.freezeProxy = frozen;
    }

    public void setProxyClassLoader(@Nullable ClassLoader classLoader) {
        this.proxyClassLoader = classLoader;
        this.classLoaderConfigured = (classLoader != null);
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        if (!this.classLoaderConfigured) {
            this.proxyClassLoader = classLoader;
        }
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        checkInterceptorNames();
    }

    @Override
    @Nullable
    public Object getObject() throws BeansException {
        initializeAdvisorChain();
        if (isSingleton()) {
            return getSingletonInstance();
        } else {
            if (this.targetName == null) {
                logger.info("Using non-singleton proxies with singleton targets is often undesirable. " + "Enable prototype proxies by setting the 'targetName' property.");
            }
            return newPrototypeInstance();
        }
    }

    @Override
    public Class<?> getObjectType() {
        synchronized (this) {
            if (this.singletonInstance != null) {
                return this.singletonInstance.getClass();
            }
        }
        Class<?>[] ifcs = getProxiedInterfaces();
        if (ifcs.length == 1) {
            return ifcs[0];
        } else if (ifcs.length > 1) {
            return createCompositeInterface(ifcs);
        } else if (this.targetName != null && this.beanFactory != null) {
            return this.beanFactory.getType(this.targetName);
        } else {
            return getTargetClass();
        }
    }

    @Override
    public boolean isSingleton() {
        return this.singleton;
    }

    protected Class<?> createCompositeInterface(Class<?>[] interfaces) {
        return ClassUtils.createCompositeInterface(interfaces, this.proxyClassLoader);
    }

    private synchronized Object getSingletonInstance() {
        if (this.singletonInstance == null) {
            this.targetSource = freshTargetSource();
            if (this.autodetectInterfaces && getProxiedInterfaces().length == 0 && !isProxyTargetClass()) {
                Class<?> targetClass = getTargetClass();
                if (targetClass == null) {
                    throw new FactoryBeanNotInitializedException("Cannot determine target class for proxy");
                }
                setInterfaces(ClassUtils.getAllInterfacesForClass(targetClass, this.proxyClassLoader));
            }
            super.setFrozen(this.freezeProxy);
            this.singletonInstance = getProxy(createAopProxy());
        }
        return this.singletonInstance;
    }

    private synchronized Object newPrototypeInstance() {
        if (logger.isTraceEnabled()) {
            logger.trace("Creating copy of prototype ProxyFactoryBean config: " + this);
        }
        ProxyCreatorSupport copy = new ProxyCreatorSupport(getAopProxyFactory());
        TargetSource targetSource = freshTargetSource();
        copy.copyConfigurationFrom(this, targetSource, freshAdvisorChain());
        if (this.autodetectInterfaces && getProxiedInterfaces().length == 0 && !isProxyTargetClass()) {
            Class<?> targetClass = targetSource.getTargetClass();
            if (targetClass != null) {
                copy.setInterfaces(ClassUtils.getAllInterfacesForClass(targetClass, this.proxyClassLoader));
            }
        }
        copy.setFrozen(this.freezeProxy);
        if (logger.isTraceEnabled()) {
            logger.trace("Using ProxyCreatorSupport copy: " + copy);
        }
        return getProxy(copy.createAopProxy());
    }

    protected Object getProxy(AopProxy aopProxy) {
        return aopProxy.getProxy(this.proxyClassLoader);
    }

    private void checkInterceptorNames() {
        if (!ObjectUtils.isEmpty(this.interceptorNames)) {
            String finalName = this.interceptorNames[this.interceptorNames.length - 1];
            if (this.targetName == null && this.targetSource == EMPTY_TARGET_SOURCE) {
                if (!finalName.endsWith(GLOBAL_SUFFIX) && !isNamedBeanAnAdvisorOrAdvice(finalName)) {
                    this.targetName = finalName;
                    if (logger.isDebugEnabled()) {
                        logger.debug("Bean with name '" + finalName + "' concluding interceptor chain " + "is not an advisor class: treating it as a target or TargetSource");
                    }
                    String[] newNames = new String[this.interceptorNames.length - 1];
                    System.arraycopy(this.interceptorNames, 0, newNames, 0, newNames.length);
                    this.interceptorNames = newNames;
                }
            }
        }
    }

    private boolean isNamedBeanAnAdvisorOrAdvice(String beanName) {
        Assert.state(this.beanFactory != null, "No BeanFactory set");
        Class<?> namedBeanClass = this.beanFactory.getType(beanName);
        if (namedBeanClass != null) {
            return (Advisor.class.isAssignableFrom(namedBeanClass) || Advice.class.isAssignableFrom(namedBeanClass));
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Could not determine type of bean with name '" + beanName + "' - assuming it is neither an Advisor nor an Advice");
        }
        return false;
    }

    private synchronized void initializeAdvisorChain() throws AopConfigException, BeansException {
        if (this.advisorChainInitialized) {
            return;
        }
        if (!ObjectUtils.isEmpty(this.interceptorNames)) {
            if (this.beanFactory == null) {
                throw new IllegalStateException("No BeanFactory available anymore (probably due to serialization) " + "- cannot resolve interceptor names " + Arrays.asList(this.interceptorNames));
            }
            if (this.interceptorNames[this.interceptorNames.length - 1].endsWith(GLOBAL_SUFFIX) && this.targetName == null && this.targetSource == EMPTY_TARGET_SOURCE) {
                throw new AopConfigException("Target required after globals");
            }
            for (String name : this.interceptorNames) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Configuring advisor or advice '" + name + "'");
                }
                if (name.endsWith(GLOBAL_SUFFIX)) {
                    if (!(this.beanFactory instanceof ListableBeanFactory)) {
                        throw new AopConfigException("Can only use global advisors or interceptors with a ListableBeanFactory");
                    }
                    addGlobalAdvisor((ListableBeanFactory) this.beanFactory, name.substring(0, name.length() - GLOBAL_SUFFIX.length()));
                } else {
                    Object advice;
                    if (this.singleton || this.beanFactory.isSingleton(name)) {
                        advice = this.beanFactory.getBean(name);
                    } else {
                        advice = new PrototypePlaceholderAdvisor(name);
                    }
                    addAdvisorOnChainCreation(advice, name);
                }
            }
        }
        this.advisorChainInitialized = true;
    }

    private List<Advisor> freshAdvisorChain() {
        Advisor[] advisors = getAdvisors();
        List<Advisor> freshAdvisors = new ArrayList<>(advisors.length);
        for (Advisor advisor : advisors) {
            if (advisor instanceof PrototypePlaceholderAdvisor) {
                PrototypePlaceholderAdvisor pa = (PrototypePlaceholderAdvisor) advisor;
                if (logger.isDebugEnabled()) {
                    logger.debug("Refreshing bean named '" + pa.getBeanName() + "'");
                }
                if (this.beanFactory == null) {
                    throw new IllegalStateException("No BeanFactory available anymore (probably due to serialization) " + "- cannot resolve prototype advisor '" + pa.getBeanName() + "'");
                }
                Object bean = this.beanFactory.getBean(pa.getBeanName());
                Advisor refreshedAdvisor = namedBeanToAdvisor(bean);
                freshAdvisors.add(refreshedAdvisor);
            } else {
                freshAdvisors.add(advisor);
            }
        }
        return freshAdvisors;
    }

    private void addGlobalAdvisor(ListableBeanFactory beanFactory, String prefix) {
        String[] globalAdvisorNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, Advisor.class);
        String[] globalInterceptorNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, Interceptor.class);
        List<Object> beans = new ArrayList<>(globalAdvisorNames.length + globalInterceptorNames.length);
        Map<Object, String> names = new HashMap<>(beans.size());
        for (String name : globalAdvisorNames) {
            Object bean = beanFactory.getBean(name);
            beans.add(bean);
            names.put(bean, name);
        }
        for (String name : globalInterceptorNames) {
            Object bean = beanFactory.getBean(name);
            beans.add(bean);
            names.put(bean, name);
        }
        AnnotationAwareOrderComparator.sort(beans);
        for (Object bean : beans) {
            String name = names.get(bean);
            if (name.startsWith(prefix)) {
                addAdvisorOnChainCreation(bean, name);
            }
        }
    }

    private void addAdvisorOnChainCreation(Object next, String name) {
        Advisor advisor = namedBeanToAdvisor(next);
        if (logger.isTraceEnabled()) {
            logger.trace("Adding advisor with name '" + name + "'");
        }
        addAdvisor(advisor);
    }

    private TargetSource freshTargetSource() {
        if (this.targetName == null) {
            if (logger.isTraceEnabled()) {
                logger.trace("Not refreshing target: Bean name not specified in 'interceptorNames'.");
            }
            return this.targetSource;
        } else {
            if (this.beanFactory == null) {
                throw new IllegalStateException("No BeanFactory available anymore (probably due to serialization) " + "- cannot resolve target with name '" + this.targetName + "'");
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Refreshing target with name '" + this.targetName + "'");
            }
            Object target = this.beanFactory.getBean(this.targetName);
            return (target instanceof TargetSource ? (TargetSource) target : new SingletonTargetSource(target));
        }
    }

    private Advisor namedBeanToAdvisor(Object next) {
        try {
            return this.advisorAdapterRegistry.wrap(next);
        } catch (UnknownAdviceTypeException ex) {
            throw new AopConfigException("Unknown advisor type " + next.getClass() + "; Can only include Advisor or Advice type beans in interceptorNames chain except for last entry," + "which may also be target or TargetSource", ex);
        }
    }

    @Override
    protected void adviceChanged() {
        super.adviceChanged();
        if (this.singleton) {
            logger.debug("Advice has changed; recaching singleton instance");
            synchronized (this) {
                this.singletonInstance = null;
            }
        }
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        this.proxyClassLoader = ClassUtils.getDefaultClassLoader();
    }

    private static class PrototypePlaceholderAdvisor implements Advisor, Serializable {

        private final String beanName;

        private final String message;

        public PrototypePlaceholderAdvisor(String beanName) {
            this.beanName = beanName;
            this.message = "Placeholder for prototype Advisor/Advice with bean name '" + beanName + "'";
        }

        public String getBeanName() {
            return this.beanName;
        }

        @Override
        public Advice getAdvice() {
            throw new UnsupportedOperationException("Cannot invoke methods: " + this.message);
        }

        @Override
        public boolean isPerInstance() {
            throw new UnsupportedOperationException("Cannot invoke methods: " + this.message);
        }

        @Override
        public String toString() {
            return this.message;
        }

    }

}
