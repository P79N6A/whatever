package org.springframework.beans.factory.support;

import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;

public class SimpleInstantiationStrategy implements InstantiationStrategy {

    private static final ThreadLocal<Method> currentlyInvokedFactoryMethod = new ThreadLocal<>();

    @Nullable
    public static Method getCurrentlyInvokedFactoryMethod() {
        return currentlyInvokedFactoryMethod.get();
    }

    @Override
    public Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner) {
        // 如果没有方法覆写，使用反射进行实例化，否则使用CGLIB
        // Don't override the class with CGLIB if no overrides.
        if (!bd.hasMethodOverrides()) {
            Constructor<?> constructorToUse;
            synchronized (bd.constructorArgumentLock) {
                constructorToUse = (Constructor<?>) bd.resolvedConstructorOrFactoryMethod;
                if (constructorToUse == null) {
                    final Class<?> clazz = bd.getBeanClass();
                    if (clazz.isInterface()) {
                        throw new BeanInstantiationException(clazz, "Specified class is an interface");
                    }
                    try {
                        if (System.getSecurityManager() != null) {
                            constructorToUse = AccessController.doPrivileged((PrivilegedExceptionAction<Constructor<?>>) clazz::getDeclaredConstructor);
                        } else {
                            constructorToUse = clazz.getDeclaredConstructor();
                        }
                        bd.resolvedConstructorOrFactoryMethod = constructorToUse;
                    } catch (Throwable ex) {
                        throw new BeanInstantiationException(clazz, "No default constructor found", ex);
                    }
                }
            }
            // 利用构造方法进行实例化
            return BeanUtils.instantiateClass(constructorToUse);
        }
        // 存在方法覆写，利用CGLIB生成子类来完成实例化
        else {
            // Must generate CGLIB subclass.
            return instantiateWithMethodInjection(bd, beanName, owner);
        }
    }

    protected Object instantiateWithMethodInjection(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner) {
        throw new UnsupportedOperationException("Method Injection not supported in SimpleInstantiationStrategy");
    }

    @Override
    public Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner, final Constructor<?> ctor, Object... args) {
        if (!bd.hasMethodOverrides()) {
            if (System.getSecurityManager() != null) {
                // use own privileged to change accessibility (when security is on)
                AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
                    ReflectionUtils.makeAccessible(ctor);
                    return null;
                });
            }
            return BeanUtils.instantiateClass(ctor, args);
        } else {
            return instantiateWithMethodInjection(bd, beanName, owner, ctor, args);
        }
    }

    protected Object instantiateWithMethodInjection(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner, @Nullable Constructor<?> ctor, Object... args) {
        throw new UnsupportedOperationException("Method Injection not supported in SimpleInstantiationStrategy");
    }

    @Override
    public Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner, @Nullable Object factoryBean, final Method factoryMethod, Object... args) {
        try {
            if (System.getSecurityManager() != null) {
                AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
                    ReflectionUtils.makeAccessible(factoryMethod);
                    return null;
                });
            } else {
                ReflectionUtils.makeAccessible(factoryMethod);
            }
            Method priorInvokedFactoryMethod = currentlyInvokedFactoryMethod.get();
            try {
                currentlyInvokedFactoryMethod.set(factoryMethod);
                Object result = factoryMethod.invoke(factoryBean, args);
                if (result == null) {
                    result = new NullBean();
                }
                return result;
            } finally {
                if (priorInvokedFactoryMethod != null) {
                    currentlyInvokedFactoryMethod.set(priorInvokedFactoryMethod);
                } else {
                    currentlyInvokedFactoryMethod.remove();
                }
            }
        } catch (IllegalArgumentException ex) {
            throw new BeanInstantiationException(factoryMethod, "Illegal arguments to factory method '" + factoryMethod.getName() + "'; " + "args: " + StringUtils.arrayToCommaDelimitedString(args), ex);
        } catch (IllegalAccessException ex) {
            throw new BeanInstantiationException(factoryMethod, "Cannot access factory method '" + factoryMethod.getName() + "'; is it public?", ex);
        } catch (InvocationTargetException ex) {
            String msg = "Factory method '" + factoryMethod.getName() + "' threw exception";
            if (bd.getFactoryBeanName() != null && owner instanceof ConfigurableBeanFactory && ((ConfigurableBeanFactory) owner).isCurrentlyInCreation(bd.getFactoryBeanName())) {
                msg = "Circular reference involving containing bean '" + bd.getFactoryBeanName() + "' - consider " + "declaring the factory method as static for independence from its containing instance. " + msg;
            }
            throw new BeanInstantiationException(factoryMethod, msg, ex.getTargetException());
        }
    }

}
