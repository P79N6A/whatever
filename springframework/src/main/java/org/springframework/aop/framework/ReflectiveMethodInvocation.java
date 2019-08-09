package org.springframework.aop.framework;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.lang.Nullable;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReflectiveMethodInvocation implements ProxyMethodInvocation, Cloneable {

    protected final Object proxy;

    @Nullable
    protected final Object target;

    protected final Method method;

    protected Object[] arguments = new Object[0];

    @Nullable
    private final Class<?> targetClass;

    @Nullable
    private Map<String, Object> userAttributes;

    protected final List<?> interceptorsAndDynamicMethodMatchers;

    private int currentInterceptorIndex = -1;

    protected ReflectiveMethodInvocation(Object proxy, @Nullable Object target, Method method, @Nullable Object[] arguments, @Nullable Class<?> targetClass, List<Object> interceptorsAndDynamicMethodMatchers) {
        this.proxy = proxy;
        this.target = target;
        this.targetClass = targetClass;
        this.method = BridgeMethodResolver.findBridgedMethod(method);
        this.arguments = AopProxyUtils.adaptArgumentsIfNecessary(method, arguments);
        this.interceptorsAndDynamicMethodMatchers = interceptorsAndDynamicMethodMatchers;
    }

    @Override
    public final Object getProxy() {
        return this.proxy;
    }

    @Override
    @Nullable
    public final Object getThis() {
        return this.target;
    }

    @Override
    public final AccessibleObject getStaticPart() {
        return this.method;
    }

    @Override
    public final Method getMethod() {
        return this.method;
    }

    @Override
    public final Object[] getArguments() {
        return this.arguments;
    }

    @Override
    public void setArguments(Object... arguments) {
        this.arguments = arguments;
    }


    @Override
    @Nullable
    public Object proceed() throws Throwable {
        // 链接链执行终点
        if (this.currentInterceptorIndex == this.interceptorsAndDynamicMethodMatchers.size() - 1) {
            // 调用被代理对象的真实方法，递归终止
            return invokeJoinpoint();
        }
        // 获取下一个拦截器
        Object interceptorOrInterceptionAdvice = this.interceptorsAndDynamicMethodMatchers.get(++this.currentInterceptorIndex);

        // 需要动态匹配
        if (interceptorOrInterceptionAdvice instanceof InterceptorAndDynamicMethodMatcher) {
            // 转型
            InterceptorAndDynamicMethodMatcher dm = (InterceptorAndDynamicMethodMatcher) interceptorOrInterceptionAdvice;
            Class<?> targetClass = (this.targetClass != null ? this.targetClass : this.method.getDeclaringClass());
            // 匹配成功
            if (dm.methodMatcher.matches(this.method, targetClass, this.arguments)) {
                // 执行
                return dm.interceptor.invoke(this);
            }
            // 匹配失败
            else {
                // 跳过，递归执行下一个
                return proceed();
            }
        }
        // 直接invoke
        else {
            // 传入this
            return ((MethodInterceptor) interceptorOrInterceptionAdvice).invoke(this);
        }
    }

    @Nullable
    protected Object invokeJoinpoint() throws Throwable {
        return AopUtils.invokeJoinpointUsingReflection(this.target, this.method, this.arguments);
    }

    @Override
    public MethodInvocation invocableClone() {
        Object[] cloneArguments = this.arguments;
        if (this.arguments.length > 0) {
            cloneArguments = new Object[this.arguments.length];
            System.arraycopy(this.arguments, 0, cloneArguments, 0, this.arguments.length);
        }
        return invocableClone(cloneArguments);
    }

    @Override
    public MethodInvocation invocableClone(Object... arguments) {
        if (this.userAttributes == null) {
            this.userAttributes = new HashMap<>();
        }
        try {
            ReflectiveMethodInvocation clone = (ReflectiveMethodInvocation) clone();
            clone.arguments = arguments;
            return clone;
        } catch (CloneNotSupportedException ex) {
            throw new IllegalStateException("Should be able to clone object of type [" + getClass() + "]: " + ex);
        }
    }

    @Override
    public void setUserAttribute(String key, @Nullable Object value) {
        if (value != null) {
            if (this.userAttributes == null) {
                this.userAttributes = new HashMap<>();
            }
            this.userAttributes.put(key, value);
        } else {
            if (this.userAttributes != null) {
                this.userAttributes.remove(key);
            }
        }
    }

    @Override
    @Nullable
    public Object getUserAttribute(String key) {
        return (this.userAttributes != null ? this.userAttributes.get(key) : null);
    }

    public Map<String, Object> getUserAttributes() {
        if (this.userAttributes == null) {
            this.userAttributes = new HashMap<>();
        }
        return this.userAttributes;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ReflectiveMethodInvocation: ");
        sb.append(this.method).append("; ");
        if (this.target == null) {
            sb.append("target is null");
        } else {
            sb.append("target is of class [").append(this.target.getClass().getName()).append(']');
        }
        return sb.toString();
    }

}
