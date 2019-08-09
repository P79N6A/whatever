package org.springframework.aop.framework.autoproxy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.Advisor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * 从BeanFactory获取所有AdvisorBean（实现了Advisor接口）
 */
public class BeanFactoryAdvisorRetrievalHelper {

    private static final Log logger = LogFactory.getLog(BeanFactoryAdvisorRetrievalHelper.class);

    private final ConfigurableListableBeanFactory beanFactory;

    /**
     * 缓存
     */
    @Nullable
    private volatile String[] cachedAdvisorBeanNames;

    public BeanFactoryAdvisorRetrievalHelper(ConfigurableListableBeanFactory beanFactory) {
        Assert.notNull(beanFactory, "ListableBeanFactory must not be null");
        this.beanFactory = beanFactory;
    }

    /**
     * 从BeanFactory中找所有符合条件的Advisor，忽略FactoryBean和正在创建中的Bean
     */
    public List<Advisor> findAdvisorBeans() {
        String[] advisorNames = this.cachedAdvisorBeanNames;
        // 如果缓存为空，从容器以及其父容器得到所有AdvisorBean的名称
        if (advisorNames == null) {
            /*
             * 从当前BeanFactory及父BeanFactory获取所有类型为Advisor的BeanName
             * includeNonSingletons=true，包含单例，非单例Bean
             * allowEagerInit=false，不初始化懒加载的单例和FactoryBean创建的Bean
             */
            advisorNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(this.beanFactory, Advisor.class, true, false);
            // 加入缓存
            this.cachedAdvisorBeanNames = advisorNames;
        }
        if (advisorNames.length == 0) {
            return new ArrayList<>();
        }
        // 存放返回值
        List<Advisor> advisors = new ArrayList<>();
        // 遍历
        for (String name : advisorNames) {
            // 检查是否符合条件，默认true，子类可重写
            if (isEligibleBean(name)) {
                // 忽略正在创建中的Bean
                if (this.beanFactory.isCurrentlyInCreation(name)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Skipping currently created advisor '" + name + "'");
                    }
                }
                // 符合条件
                else {
                    try {
                        // 实例化，加入
                        advisors.add(this.beanFactory.getBean(name, Advisor.class));
                    } catch (BeanCreationException ex) {
                        Throwable rootCause = ex.getMostSpecificCause();
                        // 如果创建过程中遇到异常是因为其依赖Bean正在创建中
                        if (rootCause instanceof BeanCurrentlyInCreationException) {
                            BeanCreationException bce = (BeanCreationException) rootCause;
                            String bceBeanName = bce.getBeanName();
                            if (bceBeanName != null && this.beanFactory.isCurrentlyInCreation(bceBeanName)) {
                                if (logger.isTraceEnabled()) {
                                    logger.trace("Skipping advisor '" + name + "' with dependency on currently created bean: " + ex.getMessage());
                                }
                                // 先忽略该Bean
                                continue;
                            }
                        }
                        // 其他异常，抛出异常并中断方法
                        throw ex;
                    }
                }
            }
        }
        return advisors;
    }

    protected boolean isEligibleBean(String beanName) {
        return true;
    }

}
