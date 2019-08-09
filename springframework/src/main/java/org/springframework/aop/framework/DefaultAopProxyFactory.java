package org.springframework.aop.framework;

import org.springframework.aop.SpringProxy;

import java.io.Serializable;
import java.lang.reflect.Proxy;

@SuppressWarnings("serial")
public class DefaultAopProxyFactory implements AopProxyFactory, Serializable {

    /**
     * 创建代理
     * 如果被代理的目标类实现了自定义接口，使用JDK动态代理（只有接口中的方法会被增强）
     * 如果没实现任何接口，使用CGLIB实现代理（基于类继承，方法不能是final或private）
     * 如果设置了proxyTargetClass=true，全都使用CGLIB
     */
    @Override
    public AopProxy createAopProxy(AdvisedSupport config) throws AopConfigException {
        // Optimize，默认false || proxy-target-class=true || 没有实现接口
        if (config.isOptimize() || config.isProxyTargetClass() || hasNoUserSuppliedProxyInterfaces(config)) {
            Class<?> targetClass = config.getTargetClass();
            if (targetClass == null) {
                throw new AopConfigException("TargetSource cannot determine target class: " + "Either an interface or a target is required for proxy creation.");
            }
            // 如果要代理的类本身就是接口，用JDK动态代理
            if (targetClass.isInterface() || Proxy.isProxyClass(targetClass)) {
                return new JdkDynamicAopProxy(config);
            }
            // CGLIB
            return new ObjenesisCglibAopProxy(config);
        }
        // 如果有接口
        else {
            return new JdkDynamicAopProxy(config);
        }
    }

    /**
     * 是否有实现自定义的接口
     */
    private boolean hasNoUserSuppliedProxyInterfaces(AdvisedSupport config) {
        Class<?>[] ifcs = config.getProxiedInterfaces();
        return (ifcs.length == 0 || (ifcs.length == 1 && SpringProxy.class.isAssignableFrom(ifcs[0])));
    }

}
