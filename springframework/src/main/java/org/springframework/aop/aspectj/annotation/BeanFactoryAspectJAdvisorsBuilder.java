package org.springframework.aop.aspectj.annotation;

import org.aspectj.lang.reflect.PerClauseKind;
import org.springframework.aop.Advisor;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 从BeanFactory中获取所有@AspectJ注解的Bean
 */
public class BeanFactoryAspectJAdvisorsBuilder {

    private final ListableBeanFactory beanFactory;

    private final AspectJAdvisorFactory advisorFactory;

    @Nullable
    private volatile List<String> aspectBeanNames;

    /**
     * 缓存
     */
    private final Map<String, List<Advisor>> advisorsCache = new ConcurrentHashMap<>();

    /**
     * 缓存
     */
    private final Map<String, MetadataAwareAspectInstanceFactory> aspectFactoryCache = new ConcurrentHashMap<>();

    public BeanFactoryAspectJAdvisorsBuilder(ListableBeanFactory beanFactory) {
        this(beanFactory, new ReflectiveAspectJAdvisorFactory(beanFactory));
    }

    public BeanFactoryAspectJAdvisorsBuilder(ListableBeanFactory beanFactory, AspectJAdvisorFactory advisorFactory) {
        Assert.notNull(beanFactory, "ListableBeanFactory must not be null");
        Assert.notNull(advisorFactory, "AspectJAdvisorFactory must not be null");
        this.beanFactory = beanFactory;
        this.advisorFactory = advisorFactory;
    }

    /**
     * 查找容器中所有被@AspectJ注解的Bean，将其中每个Advice方法包装成Advisor返回
     */
    public List<Advisor> buildAspectJAdvisors() {
        List<String> aspectNames = this.aspectBeanNames;
        // 第一次进来
        if (aspectNames == null) {
            synchronized (this) {
                aspectNames = this.aspectBeanNames;
                if (aspectNames == null) {
                    List<Advisor> advisors = new ArrayList<>();
                    aspectNames = new ArrayList<>();
                    /*
                     * 获取所有类型为Object的BeanName，就是所有的BeanName
                     * includeNonSingletons=true，包含单例，非单例Bean
                     * allowEagerInit=false，不初始化懒加载的单例和FactoryBean创建的Bean
                     */
                    String[] beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(this.beanFactory, Object.class, true, false);
                    // 遍历所有Bean
                    for (String beanName : beanNames) {
                        // 默认true，子类可重写
                        if (!isEligibleBean(beanName)) {
                            continue;
                        }
                        Class<?> beanType = this.beanFactory.getType(beanName);
                        if (beanType == null) {
                            continue;
                        }
                        // Bean被@AspectJ注解
                        if (this.advisorFactory.isAspect(beanType)) {
                            // 加进去
                            aspectNames.add(beanName);
                            // 切面元数据
                            AspectMetadata amd = new AspectMetadata(beanType, beanName);
                            // 一般都是这个
                            if (amd.getAjType().getPerClause().getKind() == PerClauseKind.SINGLETON) {
                                MetadataAwareAspectInstanceFactory factory = new BeanFactoryAspectInstanceFactory(this.beanFactory, beanName);
                                // 从被@AspectJ注解的切面类分析其Advice方法，每个构造成一个Advisor，包含Pointcut和Advice信息
                                List<Advisor> classAdvisors = this.advisorFactory.getAdvisors(factory);
                                // 切面类是单例
                                if (this.beanFactory.isSingleton(beanName)) {
                                    this.advisorsCache.put(beanName, classAdvisors);
                                } else {
                                    this.aspectFactoryCache.put(beanName, factory);
                                }
                                advisors.addAll(classAdvisors);
                            }
                            else {
                                if (this.beanFactory.isSingleton(beanName)) {
                                    throw new IllegalArgumentException("Bean with name '" + beanName + "' is a singleton, but aspect instantiation model is not singleton");
                                }
                                MetadataAwareAspectInstanceFactory factory = new PrototypeAspectInstanceFactory(this.beanFactory, beanName);
                                this.aspectFactoryCache.put(beanName, factory);
                                advisors.addAll(this.advisorFactory.getAdvisors(factory));
                            }
                        }
                    }
                    this.aspectBeanNames = aspectNames;
                    return advisors;
                }
            }
        }
        if (aspectNames.isEmpty()) {
            return Collections.emptyList();
        }
        // 从缓存获取返回
        List<Advisor> advisors = new ArrayList<>();
        for (String aspectName : aspectNames) {
            List<Advisor> cachedAdvisors = this.advisorsCache.get(aspectName);
            if (cachedAdvisors != null) {
                advisors.addAll(cachedAdvisors);
            } else {
                MetadataAwareAspectInstanceFactory factory = this.aspectFactoryCache.get(aspectName);
                advisors.addAll(this.advisorFactory.getAdvisors(factory));
            }
        }
        return advisors;
    }

    protected boolean isEligibleBean(String beanName) {
        return true;
    }

}
