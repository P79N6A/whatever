package org.springframework.aop.framework.autoproxy;

import org.springframework.aop.Advisor;
import org.springframework.aop.TargetSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.List;

/**
 * 通用自动代理创建器，检测每个Bean上适用的Advisor并据此为该Bean创建AOP代理，子类可重写
 */
@SuppressWarnings("serial")
public abstract class AbstractAdvisorAutoProxyCreator extends AbstractAutoProxyCreator {

    @Nullable
    private BeanFactoryAdvisorRetrievalHelper advisorRetrievalHelper;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        // 调用父类AbstractAutoProxyCreator的方法设置BeanFactory
        super.setBeanFactory(beanFactory);
        // 检查
        if (!(beanFactory instanceof ConfigurableListableBeanFactory)) {
            throw new IllegalArgumentException("AdvisorAutoProxyCreator requires a ConfigurableListableBeanFactory: " + beanFactory);
        }
        // 初始化BeanFactory
        initBeanFactory((ConfigurableListableBeanFactory) beanFactory);
    }

    protected void initBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        // 创建一个AdvisorRetrievalHelper，用于从BeanFactory获取Advisor
        this.advisorRetrievalHelper = new BeanFactoryAdvisorRetrievalHelperAdapter(beanFactory);
    }

    /**
     * 获取所有匹配某个Bean的Advice和Advisor，没有则不创建代理
     */
    @Override
    @Nullable
    protected Object[] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String beanName, @Nullable TargetSource targetSource) {
        // 符合条件的
        List<Advisor> advisors = findEligibleAdvisors(beanClass, beanName);
        if (advisors.isEmpty()) {
            // null，没有匹配的Advisor，不为该Bean创建代理
            return DO_NOT_PROXY;
        }
        return advisors.toArray();
    }

    protected List<Advisor> findEligibleAdvisors(Class<?> beanClass, String beanName) {
        /*
         * 找到容器中所有的Advisor
         * 当前类实现：获取容器中所有Advisor
         * AnnotationAwareAspectJAutoProxyCreator重写：获取容器中所有Advisor及将每个AspectJ切面类中的每个Advice方法封装成的Advisor
         */
        List<Advisor> candidateAdvisors = findCandidateAdvisors();
        // 过滤，找到所有适用该Bean的Advisor
        List<Advisor> eligibleAdvisors = findAdvisorsThatCanApply(candidateAdvisors, beanClass, beanName);
        /*
         * 扩展Advisor，子类重写
         * AnnotationAwareAspectJAutoProxyCreator#extendAdvisors：添加一个ExposeInvocationInterceptor
         */
        extendAdvisors(eligibleAdvisors);
        if (!eligibleAdvisors.isEmpty()) {
            // 排序
            eligibleAdvisors = sortAdvisors(eligibleAdvisors);
        }
        return eligibleAdvisors;
    }

    /**
     * 找到容器中所有的Advisor，子类可重写
     */
    protected List<Advisor> findCandidateAdvisors() {
        Assert.state(this.advisorRetrievalHelper != null, "No BeanFactoryAdvisorRetrievalHelper available");
        return this.advisorRetrievalHelper.findAdvisorBeans();
    }

    protected List<Advisor> findAdvisorsThatCanApply(List<Advisor> candidateAdvisors, Class<?> beanClass, String beanName) {
        // 使用ThreadLocal保存当前BeanName
        ProxyCreationContext.setCurrentProxiedBeanName(beanName);
        try {
            // 返回需要应用到该Bean的Advisor
            return AopUtils.findAdvisorsThatCanApply(candidateAdvisors, beanClass);
        } finally {
            // 完成后清除
            ProxyCreationContext.setCurrentProxiedBeanName(null);
        }
    }

    /**
     * 返回某个Bean是否需要被代理，默认true，子类可重写
     */
    protected boolean isEligibleAdvisorBean(String beanName) {
        return true;
    }

    protected List<Advisor> sortAdvisors(List<Advisor> advisors) {
        AnnotationAwareOrderComparator.sort(advisors);
        return advisors;
    }

    /**
     * 用于给子类扩展Advisor
     */
    protected void extendAdvisors(List<Advisor> candidateAdvisors) {
    }

    @Override
    protected boolean advisorsPreFiltered() {
        return true;
    }

    /**
     * BeanFactoryAdvisorRetrievalHelper，用于从容器中获取所有的Advisor
     */
    private class BeanFactoryAdvisorRetrievalHelperAdapter extends BeanFactoryAdvisorRetrievalHelper {

        public BeanFactoryAdvisorRetrievalHelperAdapter(ConfigurableListableBeanFactory beanFactory) {
            super(beanFactory);
        }

        @Override
        protected boolean isEligibleBean(String beanName) {
            return AbstractAdvisorAutoProxyCreator.this.isEligibleAdvisorBean(beanName);
        }

    }

}
