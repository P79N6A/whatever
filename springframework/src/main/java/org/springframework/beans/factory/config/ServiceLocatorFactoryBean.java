package org.springframework.beans.factory.config;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.*;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Properties;

public class ServiceLocatorFactoryBean implements FactoryBean<Object>, BeanFactoryAware, InitializingBean {

    @Nullable
    private Class<?> serviceLocatorInterface;

    @Nullable
    private Constructor<Exception> serviceLocatorExceptionConstructor;

    @Nullable
    private Properties serviceMappings;

    @Nullable
    private ListableBeanFactory beanFactory;

    @Nullable
    private Object proxy;

    public void setServiceLocatorInterface(Class<?> interfaceType) {
        this.serviceLocatorInterface = interfaceType;
    }

    public void setServiceLocatorExceptionClass(Class<? extends Exception> serviceLocatorExceptionClass) {
        this.serviceLocatorExceptionConstructor = determineServiceLocatorExceptionConstructor(serviceLocatorExceptionClass);
    }

    public void setServiceMappings(Properties serviceMappings) {
        this.serviceMappings = serviceMappings;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        if (!(beanFactory instanceof ListableBeanFactory)) {
            throw new FatalBeanException("ServiceLocatorFactoryBean needs to run in a BeanFactory that is a ListableBeanFactory");
        }
        this.beanFactory = (ListableBeanFactory) beanFactory;
    }

    @Override
    public void afterPropertiesSet() {
        if (this.serviceLocatorInterface == null) {
            throw new IllegalArgumentException("Property 'serviceLocatorInterface' is required");
        }
        // Create service locator proxy.
        this.proxy = Proxy.newProxyInstance(this.serviceLocatorInterface.getClassLoader(), new Class<?>[]{this.serviceLocatorInterface}, new ServiceLocatorInvocationHandler());
    }

    @SuppressWarnings("unchecked")
    protected Constructor<Exception> determineServiceLocatorExceptionConstructor(Class<? extends Exception> exceptionClass) {
        try {
            return (Constructor<Exception>) exceptionClass.getConstructor(String.class, Throwable.class);
        } catch (NoSuchMethodException ex) {
            try {
                return (Constructor<Exception>) exceptionClass.getConstructor(Throwable.class);
            } catch (NoSuchMethodException ex2) {
                try {
                    return (Constructor<Exception>) exceptionClass.getConstructor(String.class);
                } catch (NoSuchMethodException ex3) {
                    throw new IllegalArgumentException("Service locator exception [" + exceptionClass.getName() + "] neither has a (String, Throwable) constructor nor a (String) constructor");
                }
            }
        }
    }

    protected Exception createServiceLocatorException(Constructor<Exception> exceptionConstructor, BeansException cause) {
        Class<?>[] paramTypes = exceptionConstructor.getParameterTypes();
        Object[] args = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            if (String.class == paramTypes[i]) {
                args[i] = cause.getMessage();
            } else if (paramTypes[i].isInstance(cause)) {
                args[i] = cause;
            }
        }
        return BeanUtils.instantiateClass(exceptionConstructor, args);
    }

    @Override
    @Nullable
    public Object getObject() {
        return this.proxy;
    }

    @Override
    public Class<?> getObjectType() {
        return this.serviceLocatorInterface;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    private class ServiceLocatorInvocationHandler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (ReflectionUtils.isEqualsMethod(method)) {
                // Only consider equal when proxies are identical.
                return (proxy == args[0]);
            } else if (ReflectionUtils.isHashCodeMethod(method)) {
                // Use hashCode of service locator proxy.
                return System.identityHashCode(proxy);
            } else if (ReflectionUtils.isToStringMethod(method)) {
                return "Service locator: " + serviceLocatorInterface;
            } else {
                return invokeServiceLocatorMethod(method, args);
            }
        }

        private Object invokeServiceLocatorMethod(Method method, Object[] args) throws Exception {
            Class<?> serviceLocatorMethodReturnType = getServiceLocatorMethodReturnType(method);
            try {
                String beanName = tryGetBeanName(args);
                Assert.state(beanFactory != null, "No BeanFactory available");
                if (StringUtils.hasLength(beanName)) {
                    // Service locator for a specific bean name
                    return beanFactory.getBean(beanName, serviceLocatorMethodReturnType);
                } else {
                    // Service locator for a bean type
                    return beanFactory.getBean(serviceLocatorMethodReturnType);
                }
            } catch (BeansException ex) {
                if (serviceLocatorExceptionConstructor != null) {
                    throw createServiceLocatorException(serviceLocatorExceptionConstructor, ex);
                }
                throw ex;
            }
        }

        private String tryGetBeanName(@Nullable Object[] args) {
            String beanName = "";
            if (args != null && args.length == 1 && args[0] != null) {
                beanName = args[0].toString();
            }
            // Look for explicit serviceId-to-beanName mappings.
            if (serviceMappings != null) {
                String mappedName = serviceMappings.getProperty(beanName);
                if (mappedName != null) {
                    beanName = mappedName;
                }
            }
            return beanName;
        }

        private Class<?> getServiceLocatorMethodReturnType(Method method) throws NoSuchMethodException {
            Assert.state(serviceLocatorInterface != null, "No service locator interface specified");
            Class<?>[] paramTypes = method.getParameterTypes();
            Method interfaceMethod = serviceLocatorInterface.getMethod(method.getName(), paramTypes);
            Class<?> serviceLocatorReturnType = interfaceMethod.getReturnType();
            // Check whether the method is a valid service locator.
            if (paramTypes.length > 1 || void.class == serviceLocatorReturnType) {
                throw new UnsupportedOperationException("May only call methods with signature '<type> xxx()' or '<type> xxx(<idtype> id)' " + "on factory interface, but tried to call: " + interfaceMethod);
            }
            return serviceLocatorReturnType;
        }

    }

}
