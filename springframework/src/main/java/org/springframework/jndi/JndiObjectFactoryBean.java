package org.springframework.jndi;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import javax.naming.Context;
import javax.naming.NamingException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class JndiObjectFactoryBean extends JndiObjectLocator implements FactoryBean<Object>, BeanFactoryAware, BeanClassLoaderAware {

    @Nullable
    private Class<?>[] proxyInterfaces;

    private boolean lookupOnStartup = true;

    private boolean cache = true;

    private boolean exposeAccessContext = false;

    @Nullable
    private Object defaultObject;

    @Nullable
    private ConfigurableBeanFactory beanFactory;

    @Nullable
    private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

    @Nullable
    private Object jndiObject;

    public void setProxyInterface(Class<?> proxyInterface) {
        this.proxyInterfaces = new Class<?>[]{proxyInterface};
    }

    public void setProxyInterfaces(Class<?>... proxyInterfaces) {
        this.proxyInterfaces = proxyInterfaces;
    }

    public void setLookupOnStartup(boolean lookupOnStartup) {
        this.lookupOnStartup = lookupOnStartup;
    }

    public void setCache(boolean cache) {
        this.cache = cache;
    }

    public void setExposeAccessContext(boolean exposeAccessContext) {
        this.exposeAccessContext = exposeAccessContext;
    }

    public void setDefaultObject(Object defaultObject) {
        this.defaultObject = defaultObject;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        if (beanFactory instanceof ConfigurableBeanFactory) {
            // Just optional - for getting a specifically configured TypeConverter if needed.
            // We'll simply fall back to a SimpleTypeConverter if no specific one available.
            this.beanFactory = (ConfigurableBeanFactory) beanFactory;
        }
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.beanClassLoader = classLoader;
    }

    @Override
    public void afterPropertiesSet() throws IllegalArgumentException, NamingException {
        super.afterPropertiesSet();
        if (this.proxyInterfaces != null || !this.lookupOnStartup || !this.cache || this.exposeAccessContext) {
            // We need to create a proxy for this...
            if (this.defaultObject != null) {
                throw new IllegalArgumentException("'defaultObject' is not supported in combination with 'proxyInterface'");
            }
            // We need a proxy and a JndiObjectTargetSource.
            this.jndiObject = JndiObjectProxyFactory.createJndiObjectProxy(this);
        } else {
            if (this.defaultObject != null && getExpectedType() != null && !getExpectedType().isInstance(this.defaultObject)) {
                TypeConverter converter = (this.beanFactory != null ? this.beanFactory.getTypeConverter() : new SimpleTypeConverter());
                try {
                    this.defaultObject = converter.convertIfNecessary(this.defaultObject, getExpectedType());
                } catch (TypeMismatchException ex) {
                    throw new IllegalArgumentException("Default object [" + this.defaultObject + "] of type [" + this.defaultObject.getClass().getName() + "] is not of expected type [" + getExpectedType().getName() + "] and cannot be converted either", ex);
                }
            }
            // Locate specified JNDI object.
            this.jndiObject = lookupWithFallback();
        }
    }

    protected Object lookupWithFallback() throws NamingException {
        ClassLoader originalClassLoader = ClassUtils.overrideThreadContextClassLoader(this.beanClassLoader);
        try {
            return lookup();
        } catch (TypeMismatchNamingException ex) {
            // Always let TypeMismatchNamingException through -
            // we don't want to fall back to the defaultObject in this case.
            throw ex;
        } catch (NamingException ex) {
            if (this.defaultObject != null) {
                if (logger.isTraceEnabled()) {
                    logger.trace("JNDI lookup failed - returning specified default object instead", ex);
                } else if (logger.isDebugEnabled()) {
                    logger.debug("JNDI lookup failed - returning specified default object instead: " + ex);
                }
                return this.defaultObject;
            }
            throw ex;
        } finally {
            if (originalClassLoader != null) {
                Thread.currentThread().setContextClassLoader(originalClassLoader);
            }
        }
    }

    @Override
    @Nullable
    public Object getObject() {
        return this.jndiObject;
    }

    @Override
    public Class<?> getObjectType() {
        if (this.proxyInterfaces != null) {
            if (this.proxyInterfaces.length == 1) {
                return this.proxyInterfaces[0];
            } else if (this.proxyInterfaces.length > 1) {
                return createCompositeInterface(this.proxyInterfaces);
            }
        }
        if (this.jndiObject != null) {
            return this.jndiObject.getClass();
        } else {
            return getExpectedType();
        }
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    protected Class<?> createCompositeInterface(Class<?>[] interfaces) {
        return ClassUtils.createCompositeInterface(interfaces, this.beanClassLoader);
    }

    private static class JndiObjectProxyFactory {

        private static Object createJndiObjectProxy(JndiObjectFactoryBean jof) throws NamingException {
            // Create a JndiObjectTargetSource that mirrors the JndiObjectFactoryBean's configuration.
            JndiObjectTargetSource targetSource = new JndiObjectTargetSource();
            targetSource.setJndiTemplate(jof.getJndiTemplate());
            String jndiName = jof.getJndiName();
            Assert.state(jndiName != null, "No JNDI name specified");
            targetSource.setJndiName(jndiName);
            targetSource.setExpectedType(jof.getExpectedType());
            targetSource.setResourceRef(jof.isResourceRef());
            targetSource.setLookupOnStartup(jof.lookupOnStartup);
            targetSource.setCache(jof.cache);
            targetSource.afterPropertiesSet();
            // Create a proxy with JndiObjectFactoryBean's proxy interface and the JndiObjectTargetSource.
            ProxyFactory proxyFactory = new ProxyFactory();
            if (jof.proxyInterfaces != null) {
                proxyFactory.setInterfaces(jof.proxyInterfaces);
            } else {
                Class<?> targetClass = targetSource.getTargetClass();
                if (targetClass == null) {
                    throw new IllegalStateException("Cannot deactivate 'lookupOnStartup' without specifying a 'proxyInterface' or 'expectedType'");
                }
                Class<?>[] ifcs = ClassUtils.getAllInterfacesForClass(targetClass, jof.beanClassLoader);
                for (Class<?> ifc : ifcs) {
                    if (Modifier.isPublic(ifc.getModifiers())) {
                        proxyFactory.addInterface(ifc);
                    }
                }
            }
            if (jof.exposeAccessContext) {
                proxyFactory.addAdvice(new JndiContextExposingInterceptor(jof.getJndiTemplate()));
            }
            proxyFactory.setTargetSource(targetSource);
            return proxyFactory.getProxy(jof.beanClassLoader);
        }

    }

    private static class JndiContextExposingInterceptor implements MethodInterceptor {

        private final JndiTemplate jndiTemplate;

        public JndiContextExposingInterceptor(JndiTemplate jndiTemplate) {
            this.jndiTemplate = jndiTemplate;
        }

        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            Context ctx = (isEligible(invocation.getMethod()) ? this.jndiTemplate.getContext() : null);
            try {
                return invocation.proceed();
            } finally {
                this.jndiTemplate.releaseContext(ctx);
            }
        }

        protected boolean isEligible(Method method) {
            return (Object.class != method.getDeclaringClass());
        }

    }

}
