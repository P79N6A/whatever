package org.springframework.aop.framework.autoproxy;

import org.springframework.aop.framework.AbstractAdvisingBeanPostProcessor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.lang.Nullable;

@SuppressWarnings("serial")
public abstract class AbstractBeanFactoryAwareAdvisingPostProcessor extends AbstractAdvisingBeanPostProcessor implements BeanFactoryAware {

    @Nullable
    private ConfigurableListableBeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = (beanFactory instanceof ConfigurableListableBeanFactory ? (ConfigurableListableBeanFactory) beanFactory : null);
    }

    @Override
    protected ProxyFactory prepareProxyFactory(Object bean, String beanName) {
        if (this.beanFactory != null) {
            AutoProxyUtils.exposeTargetClass(this.beanFactory, beanName, bean.getClass());
        }
        ProxyFactory proxyFactory = super.prepareProxyFactory(bean, beanName);
        if (!proxyFactory.isProxyTargetClass() && this.beanFactory != null && AutoProxyUtils.shouldProxyTargetClass(this.beanFactory, beanName)) {
            proxyFactory.setProxyTargetClass(true);
        }
        return proxyFactory;
    }

    /**
     * 是否符合条件
     */
    @Override
    protected boolean isEligible(Object bean, String beanName) {
        // beanName不以.ORIGINAL结尾 && 父类方法也匹配
        return (!AutoProxyUtils.isOriginalInstance(beanName, bean.getClass()) && super.isEligible(bean, beanName));
    }

}
