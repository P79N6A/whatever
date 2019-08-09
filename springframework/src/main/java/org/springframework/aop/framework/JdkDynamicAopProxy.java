package org.springframework.aop.framework;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.AopInvocationException;
import org.springframework.aop.RawTargetAccess;
import org.springframework.aop.TargetSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.DecoratingProxy;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

final class JdkDynamicAopProxy implements AopProxy, InvocationHandler, Serializable {

    private static final long serialVersionUID = 5531744639992436476L;

    private static final Log logger = LogFactory.getLog(JdkDynamicAopProxy.class);

    private final AdvisedSupport advised;

    private boolean equalsDefined;

    private boolean hashCodeDefined;

    public JdkDynamicAopProxy(AdvisedSupport config) throws AopConfigException {
        Assert.notNull(config, "AdvisedSupport must not be null");
        if (config.getAdvisors().length == 0 && config.getTargetSource() == AdvisedSupport.EMPTY_TARGET_SOURCE) {
            throw new AopConfigException("No advisors and no TargetSource specified");
        }
        this.advised = config;
    }

    @Override
    public Object getProxy() {
        return getProxy(ClassUtils.getDefaultClassLoader());
    }

    @Override
    public Object getProxy(@Nullable ClassLoader classLoader) {
        if (logger.isTraceEnabled()) {
            logger.trace("Creating JDK dynamic proxy: " + this.advised.getTargetSource());
        }
        Class<?>[] proxiedInterfaces = AopProxyUtils.completeProxiedInterfaces(this.advised, true);
        // 遍历确定有没有实现equals和hashCode方法并缓存
        findDefinedEqualsAndHashCodeMethods(proxiedInterfaces);
        // 返回代理对象
        return Proxy.newProxyInstance(classLoader, proxiedInterfaces, this);
    }

    private void findDefinedEqualsAndHashCodeMethods(Class<?>[] proxiedInterfaces) {
        for (Class<?> proxiedInterface : proxiedInterfaces) {
            Method[] methods = proxiedInterface.getDeclaredMethods();
            for (Method method : methods) {
                if (AopUtils.isEqualsMethod(method)) {
                    this.equalsDefined = true;
                }
                if (AopUtils.isHashCodeMethod(method)) {
                    this.hashCodeDefined = true;
                }
                if (this.equalsDefined && this.hashCodeDefined) {
                    return;
                }
            }
        }
    }

    @Override
    @Nullable
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        MethodInvocation invocation;
        Object oldProxy = null;
        boolean setProxyContext = false;
        TargetSource targetSource = this.advised.targetSource;
        Object target = null;
        try {
            // 没有声明equals方法 && 调用equals方法
            if (!this.equalsDefined && AopUtils.isEqualsMethod(method)) {
                // 调用JdkDynamicAopProxy的equals方法
                return equals(args[0]);
            }
            // 没有声明hashCode方法 && 调用hashCode方法
            else if (!this.hashCodeDefined && AopUtils.isHashCodeMethod(method)) {
                // 调用JdkDynamicAopProxy的equals方法
                return hashCode();
            }
            // 调用的方法是DecoratingProxy声明的方法
            else if (method.getDeclaringClass() == DecoratingProxy.class) {
                // DecoratingProxy只有一个getDecoratedClass方法，直接返回被装饰的Class
                return AopProxyUtils.ultimateTargetClass(this.advised);
            }
            // 代理不是不透明的 && 是接口声明的方法 && 声明该方法的接口继承了Advised接口
            else if (!this.advised.opaque && method.getDeclaringClass().isInterface() && method.getDeclaringClass().isAssignableFrom(Advised.class)) {
                // 直接调用
                return AopUtils.invokeJoinpointUsingReflection(this.advised, method, args);
            }
            Object retVal;
            // 如果暴露代理，默认false
            if (this.advised.exposeProxy) {
                // ThreadLocal保存当前代理对象，返回原本的代理对象
                oldProxy = AopContext.setCurrentProxy(proxy);
                setProxyContext = true;
            }
            target = targetSource.getTarget();
            Class<?> targetClass = (target != null ? target.getClass() : null);
            /*
             * 遍历，把Advisor中的Advice包装成Interceptor
             * 以AspectJ为例：Advisor - Advice - MethodInterceptor
             * AspectJAroundAdvice，AspectJAfterAdvice，AtAfterThrowing直接添加
             * AspectJMethodBeforeAdvice，AspectJAfterReturningAdvice通过对应AdvisorAdapter包装成MethodInterceptor添加到尾部
             */
            List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);
            // 如果链是空
            if (chain.isEmpty()) {
                Object[] argsToUse = AopProxyUtils.adaptArgumentsIfNecessary(method, args);
                // 直接反射调用被代理对象的方法
                retVal = AopUtils.invokeJoinpointUsingReflection(target, method, argsToUse);
            }
            // 否则
            else {
                // 创建一个ReflectiveMethodInvocation
                invocation = new ReflectiveMethodInvocation(proxy, target, method, args, targetClass, chain);
                // 执行链式调用，得到返回结果
                retVal = invocation.proceed();
            }
            Class<?> returnType = method.getReturnType();
            // 返回结果不为null && 返回结果是this，即原始对象 && 方法声明类没有标记为RawTargetAccess(没继承RawTargetAccess接口)
            if (retVal != null && retVal == target && returnType != Object.class && returnType.isInstance(proxy) && !RawTargetAccess.class.isAssignableFrom(method.getDeclaringClass())) {
                // 返回代理对象
                retVal = proxy;
            } else if (retVal == null && returnType != Void.TYPE && returnType.isPrimitive()) {
                throw new AopInvocationException("Null return value from advice does not match primitive return type for: " + method);
            }
            return retVal;
        } finally {
            if (target != null && !targetSource.isStatic()) {
                targetSource.releaseTarget(target);
            }
            if (setProxyContext) {
                // 把原本的代理对象设回去
                AopContext.setCurrentProxy(oldProxy);
            }
        }
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (other == this) {
            return true;
        }
        if (other == null) {
            return false;
        }
        JdkDynamicAopProxy otherProxy;
        if (other instanceof JdkDynamicAopProxy) {
            otherProxy = (JdkDynamicAopProxy) other;
        } else if (Proxy.isProxyClass(other.getClass())) {
            InvocationHandler ih = Proxy.getInvocationHandler(other);
            if (!(ih instanceof JdkDynamicAopProxy)) {
                return false;
            }
            otherProxy = (JdkDynamicAopProxy) ih;
        } else {
            return false;
        }
        return AopProxyUtils.equalsInProxy(this.advised, otherProxy.advised);
    }

    @Override
    public int hashCode() {
        return JdkDynamicAopProxy.class.hashCode() * 13 + this.advised.getTargetSource().hashCode();
    }

}
