package org.springframework.aop.framework;

import org.springframework.aop.Advisor;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.lang.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("serial")
public abstract class AbstractAdvisingBeanPostProcessor extends ProxyProcessorSupport implements BeanPostProcessor {

    /**
     * this.advisor应用到已有advisor的最外还是最里
     * false表示应用到最里面，离目标Bean最近
     */
    protected boolean beforeExistingAdvisors = false;

    /**
     * 缓存BeanClass被添加Advisor的条件，如果没有则AopUtils#canApply检测并加入
     */
    private final Map<Class<?>, Boolean> eligibleBeans = new ConcurrentHashMap<>(256);

    /**
     * 要应用目标Bean上的Advisor
     */
    @Nullable
    protected Advisor advisor;

    protected void customizeProxyFactory(ProxyFactory proxyFactory) {
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        //
        if ((this.advisor == null) || (bean instanceof AopInfrastructureBean)) {
            // 直接返回
            return bean;
        }
        // Advised
        if (bean instanceof Advised) {
            Advised advised = (Advised) bean;
            if (!advised.isFrozen() && isEligible(AopUtils.getTargetClass(bean))) {
                if (this.beforeExistingAdvisors) {
                    advised.addAdvisor(0, this.advisor);
                } else {
                    // 添加Advisor到Advised
                    advised.addAdvisor(this.advisor);
                }
                return bean;
            }
        }
        //
        if (isEligible(bean, beanName)) {
            // 准备ProxyFactory
            ProxyFactory proxyFactory = prepareProxyFactory(bean, beanName);
            if (!proxyFactory.isProxyTargetClass()) {
                evaluateProxyInterfaces(bean.getClass(), proxyFactory);
            }
            // 添加Advisor到代理对象工厂
            proxyFactory.addAdvisor(this.advisor);
            customizeProxyFactory(proxyFactory);
            // 创建代理对象
            return proxyFactory.getProxy(getProxyClassLoader());
        }
        // 不符合条件，直接返回
        return bean;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }

    protected ProxyFactory prepareProxyFactory(Object bean, String beanName) {
        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.copyFrom(this);
        proxyFactory.setTarget(bean);
        return proxyFactory;
    }

    public void setBeforeExistingAdvisors(boolean beforeExistingAdvisors) {
        this.beforeExistingAdvisors = beforeExistingAdvisors;
    }

    protected boolean isEligible(Class<?> targetClass) {
        // 先从缓存获取
        Boolean eligible = this.eligibleBeans.get(targetClass);
        if (eligible != null) {
            // 有就直接返回
            return eligible;
        }
        if (this.advisor == null) {
            return false;
        }
        // 检测
        eligible = AopUtils.canApply(this.advisor, targetClass);
        // 加入缓存
        this.eligibleBeans.put(targetClass, eligible);
        return eligible;
    }

    /**
     * 目标Bean是否符合Advisor的条件
     */
    protected boolean isEligible(Object bean, String beanName) {
        return isEligible(bean.getClass());
    }

}

