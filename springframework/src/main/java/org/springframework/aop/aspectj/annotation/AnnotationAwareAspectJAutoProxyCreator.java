package org.springframework.aop.aspectj.annotation;

import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.autoproxy.AspectJAwareAdvisorAutoProxyCreator;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 获取容器所有被@AspectJ注解的切面类中的Advice方法和所有AdvisorBean
 */
@SuppressWarnings("serial")
public class AnnotationAwareAspectJAutoProxyCreator extends AspectJAwareAdvisorAutoProxyCreator {

    @Nullable
    private List<Pattern> includePatterns;

    @Nullable
    private AspectJAdvisorFactory aspectJAdvisorFactory;

    @Nullable
    private BeanFactoryAspectJAdvisorsBuilder aspectJAdvisorsBuilder;

    public void setIncludePatterns(List<String> patterns) {
        this.includePatterns = new ArrayList<>(patterns.size());
        for (String patternText : patterns) {
            this.includePatterns.add(Pattern.compile(patternText));
        }
    }

    public void setAspectJAdvisorFactory(AspectJAdvisorFactory aspectJAdvisorFactory) {
        Assert.notNull(aspectJAdvisorFactory, "AspectJAdvisorFactory must not be null");
        this.aspectJAdvisorFactory = aspectJAdvisorFactory;
    }

    @Override
    protected void initBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        /*
         * 调用父类AbstractAdvisorAutoProxyCreator#initBeanFactory初始化BeanFactory
         * 其实是构造一个BeanFactoryAdvisorRetrievalHelper，用于从BeanFactory获取Advisor
         */
        super.initBeanFactory(beanFactory);
        super.initBeanFactory(beanFactory);
        super.initBeanFactory(beanFactory);
        if (this.aspectJAdvisorFactory == null) {
            this.aspectJAdvisorFactory = new ReflectiveAspectJAdvisorFactory(beanFactory);
        }
        // 用于通过反射从BeanFactory获取所有被@AspectJ注解的Bean
        this.aspectJAdvisorsBuilder = new BeanFactoryAspectJAdvisorsBuilderAdapter(beanFactory, this.aspectJAdvisorFactory);
    }

    /**
     * 找到容器中所有的AdvisorBean和@AspectJ注解的Bean，封装成Advisor列表返回
     */
    @Override
    protected List<Advisor> findCandidateAdvisors() {
        // 通过父类AbstractAdvisorAutoProxyCreator查找：BeanFactoryAdvisorRetrievalHelper#findAdvisorBeans从容器中查找
        List<Advisor> advisors = super.findCandidateAdvisors();
        // 通过AspectJAdvisorsBuilder从容器中获取所有@AspectJ注解的Bean，并包装成Advisor加入
        if (this.aspectJAdvisorsBuilder != null) {
            advisors.addAll(this.aspectJAdvisorsBuilder.buildAspectJAdvisors());
        }
        return advisors;
    }

    /**
     * 判断指定的类是否是基础设置类，如果是基础设施类，不会代理该类
     */
    @Override
    protected boolean isInfrastructureClass(Class<?> beanClass) {
        // 父类是基础设施类 || @Aspect注解
        return (super.isInfrastructureClass(beanClass) || (this.aspectJAdvisorFactory != null && this.aspectJAdvisorFactory.isAspect(beanClass)));
    }

    /**
     * 判断给定名称的Bean是否是符合条件的AspectBean
     */
    protected boolean isEligibleAspectBean(String beanName) {
        // includePatterns == null
        if (this.includePatterns == null) {
            return true;
        }
        //
        else {
            for (Pattern pattern : this.includePatterns) {
                // 匹配
                if (pattern.matcher(beanName).matches()) {
                    return true;
                }
            }
            return false;
        }
    }

    private class BeanFactoryAspectJAdvisorsBuilderAdapter extends BeanFactoryAspectJAdvisorsBuilder {

        public BeanFactoryAspectJAdvisorsBuilderAdapter(ListableBeanFactory beanFactory, AspectJAdvisorFactory advisorFactory) {
            super(beanFactory, advisorFactory);
        }

        @Override
        protected boolean isEligibleBean(String beanName) {
            return AnnotationAwareAspectJAutoProxyCreator.this.isEligibleAspectBean(beanName);
        }

    }

}
