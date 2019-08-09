package org.springframework.aop.framework.autoproxy;

import org.aopalliance.aop.Advice;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.Advisor;
import org.springframework.aop.Pointcut;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.framework.ProxyProcessorSupport;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 将Bean包装成代理对象，包裹相应的AOP拦截器
 * 通用拦截器：适用于所有代理对象的拦截器
 * 专用拦截器：针对特定Bean的拦截器
 */
@SuppressWarnings("serial")
public abstract class AbstractAutoProxyCreator extends ProxyProcessorSupport implements SmartInstantiationAwareBeanPostProcessor, BeanFactoryAware {

    @Nullable
    protected static final Object[] DO_NOT_PROXY = null;

    protected static final Object[] PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS = new Object[0];

    protected final Log logger = LogFactory.getLog(getClass());

    /**
     * 将各种类型的拦截器适配成Advisor，默认DefaultAdvisorAdapterRegistry
     */
    private AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

    private boolean freezeProxy = false;

    /**
     * 通用拦截器Bean的名称，用于从容器获得相应的拦截器实例，默认为空
     */
    private String[] interceptorNames = new String[0];

    /**
     * 创建某个Bean的代理对象时，先应用通用拦截器还是先应用专用拦截器，true表示先应用通用拦截器
     */
    private boolean applyCommonInterceptorsFirst = true;

    @Nullable
    private TargetSourceCreator[] customTargetSourceCreators;

    @Nullable
    private BeanFactory beanFactory;

    /**
     * 缓存targetSource不为null的BeanName
     */
    private final Set<String> targetSourcedBeans = Collections.newSetFromMap(new ConcurrentHashMap<>(16));

    /**
     * 缓存
     */
    private final Map<Object, Object> earlyProxyReferences = new ConcurrentHashMap<>(16);

    /**
     * 缓存
     */
    private final Map<Object, Class<?>> proxyTypes = new ConcurrentHashMap<>(16);

    /**
     * 缓存，true表示该Bean已经创建代理对象，false表示已经判断该Bean不需要创建代理对象
     */
    private final Map<Object, Boolean> advisedBeans = new ConcurrentHashMap<>(256);

    @Override
    public void setFrozen(boolean frozen) {
        this.freezeProxy = frozen;
    }

    @Override
    public boolean isFrozen() {
        return this.freezeProxy;
    }

    public void setAdvisorAdapterRegistry(AdvisorAdapterRegistry advisorAdapterRegistry) {
        this.advisorAdapterRegistry = advisorAdapterRegistry;
    }

    public void setCustomTargetSourceCreators(TargetSourceCreator... targetSourceCreators) {
        this.customTargetSourceCreators = targetSourceCreators;
    }

    public void setInterceptorNames(String... interceptorNames) {
        this.interceptorNames = interceptorNames;
    }

    public void setApplyCommonInterceptorsFirst(boolean applyCommonInterceptorsFirst) {
        this.applyCommonInterceptorsFirst = applyCommonInterceptorsFirst;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Nullable
    protected BeanFactory getBeanFactory() {
        return this.beanFactory;
    }

    @Override
    @Nullable
    public Class<?> predictBeanType(Class<?> beanClass, String beanName) {
        if (this.proxyTypes.isEmpty()) {
            return null;
        }
        Object cacheKey = getCacheKey(beanClass, beanName);
        return this.proxyTypes.get(cacheKey);
    }

    @Override
    @Nullable
    public Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, String beanName) {
        return null;
    }

    @Override
    public Object getEarlyBeanReference(Object bean, String beanName) {
        // 根据beanClass和beanName创建缓存键
        Object cacheKey = getCacheKey(bean.getClass(), beanName);
        this.earlyProxyReferences.put(cacheKey, bean);
        return wrapIfNecessary(bean, beanName, cacheKey);
    }

    /**
     * Bean实例化前
     * InstantiationAwareBeanPostProcessor#postProcessBeforeInstantiation
     * 检查如果该Bean需要创建代理，并且存在自定义TargetSource，基于TargetSource和匹配的Advice/Advisor为该Bean创建代理对象
     * 这里可能会创建代理，不过一般不会，查看getCustomTargetSource，需要配置customTargetSourceCreators
     */
    @Override
    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) {
        // 根据beanClass和beanName创建缓存键
        Object cacheKey = getCacheKey(beanClass, beanName);
        // beanName为空 || targetSourcedBeans没有缓存
        if (!StringUtils.hasLength(beanName) || !this.targetSourcedBeans.contains(beanName)) {
            //
            if (this.advisedBeans.containsKey(cacheKey)) {
                // 已经被该方法处理过，不再处理
                return null;
            }
            // 基础设施类 || 应该跳过
            if (isInfrastructureClass(beanClass) || shouldSkip(beanClass, beanName)) {
                // 该Bean尚未被处理过，但如果是基础设施Bean或是需要被排除的Bean，则将它们添加到缓存中，标记为false
                this.advisedBeans.put(cacheKey, Boolean.FALSE);
                return null;
            }
        }
        TargetSource targetSource = getCustomTargetSource(beanClass, beanName);
        // 有自定义的TargetSource实现才创建代理
        if (targetSource != null) {
            if (StringUtils.hasLength(beanName)) {
                // 表明该Bean有TargetSource
                this.targetSourcedBeans.add(beanName);
            }
            // 获取针对该Bean的所有专用拦截器
            Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(beanClass, beanName, targetSource);
            // 创建该Bean的代理对象
            Object proxy = createProxy(beanClass, beanName, specificInterceptors, targetSource);
            this.proxyTypes.put(cacheKey, proxy.getClass());
            // 返回创建的代理对象，不再走主流程
            return proxy;
        }
        // 返回null表示当前方法没有做处理，继续进行
        return null;
    }

    @Override
    public boolean postProcessAfterInstantiation(Object bean, String beanName) {
        return true;
    }

    @Override
    public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) {
        return pvs;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }

    /**
     * 实例化并初始化完成后
     */
    @Override
    public Object postProcessAfterInitialization(@Nullable Object bean, String beanName) {
        if (bean != null) {
            Object cacheKey = getCacheKey(bean.getClass(), beanName);
            if (this.earlyProxyReferences.remove(cacheKey) != bean) {
                // 代理包装
                return wrapIfNecessary(bean, beanName, cacheKey);
            }
        }
        return bean;
    }

    protected Object getCacheKey(Class<?> beanClass, @Nullable String beanName) {
        // 有名字就根据名字判断
        if (StringUtils.hasLength(beanName)) {
            return (FactoryBean.class.isAssignableFrom(beanClass) ? BeanFactory.FACTORY_BEAN_PREFIX + beanName : beanName);
        }
        // 没名字就返回类
        else {
            return beanClass;
        }
    }

    /**
     * 代理包装，如果需要
     */
    protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
        // 该Bean之前已经被postProcessBeforeInstantiation处理过，不再处理
        if (StringUtils.hasLength(beanName) && this.targetSourcedBeans.contains(beanName)) {
            return bean;
        }
        // 该Bean之前已经被postProcessBeforeInstantiation标记为不需要处理
        if (Boolean.FALSE.equals(this.advisedBeans.get(cacheKey))) {
            return bean;
        }
        // 基础设施类 || 需要跳过的Bean
        if (isInfrastructureClass(bean.getClass()) || shouldSkip(bean.getClass(), beanName)) {
            // 加到缓存，标记为不需要处理
            this.advisedBeans.put(cacheKey, Boolean.FALSE);
            // 不需要代理包装
            return bean;
        }
        /*
         * 获取适用该Bean的所有专用拦截器对象，三种返回值 :
         *      有值：代理，使用专用拦截器和通用拦截器
         *      无值：代理，没有专用拦截器，仅使用通用拦截器
         *      Null：不代理
         */
        Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null);
        if (specificInterceptors != DO_NOT_PROXY) {
            // 加入缓存，表示当前Bean需要创建相应的代理对象
            this.advisedBeans.put(cacheKey, Boolean.TRUE);
            // 创建代理
            Object proxy = createProxy(bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));
            // 缓存代理对象类型
            this.proxyTypes.put(cacheKey, proxy.getClass());
            // 返回代理对象
            return proxy;
        }
        // 加入缓存，表示当前Bean不需要创建代理
        this.advisedBeans.put(cacheKey, Boolean.FALSE);
        return bean;
    }

    /**
     * 基础设施类
     * 没有继承Advice || 没有继承Pointcut || 没有继承Advisor || 没有继承AopInfrastructureBean
     */
    protected boolean isInfrastructureClass(Class<?> beanClass) {
        boolean retVal = Advice.class.isAssignableFrom(beanClass) || Pointcut.class.isAssignableFrom(beanClass) || Advisor.class.isAssignableFrom(beanClass) || AopInfrastructureBean.class.isAssignableFrom(beanClass);
        if (retVal && logger.isTraceEnabled()) {
            logger.trace("Did not attempt to auto-proxy infrastructure class [" + beanClass.getName() + "]");
        }
        return retVal;
    }

    protected boolean shouldSkip(Class<?> beanClass, String beanName) {
        return AutoProxyUtils.isOriginalInstance(beanName, beanClass);
    }

    @Nullable
    protected TargetSource getCustomTargetSource(Class<?> beanClass, String beanName) {
        if (this.customTargetSourceCreators != null && this.beanFactory != null && this.beanFactory.containsBean(beanName)) {
            for (TargetSourceCreator tsc : this.customTargetSourceCreators) {
                TargetSource ts = tsc.getTargetSource(beanClass, beanName);
                if (ts != null) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("TargetSourceCreator [" + tsc + "] found custom TargetSource for bean with name '" + beanName + "'");
                    }
                    return ts;
                }
            }
        }
        return null;
    }

    /**
     * 创建代理
     */
    protected Object createProxy(Class<?> beanClass, @Nullable String beanName, @Nullable Object[] specificInterceptors, TargetSource targetSource) {
        if (this.beanFactory instanceof ConfigurableListableBeanFactory) {
            /*
             * 暴露目标类：
             * 在BeanDefinition设置属性org.springframework.aop.framework.autoproxy.AutoProxyUtils.originalTargetClass为原始类
             */
            AutoProxyUtils.exposeTargetClass((ConfigurableListableBeanFactory) this.beanFactory, beanName, beanClass);
        }
        // 创建ProxyFactory
        ProxyFactory proxyFactory = new ProxyFactory();
        // 复制到ProxyConfig
        proxyFactory.copyFrom(this);
        // 如果设置proxyTargetClass=true，则不管有没有接口，都使用CGLIB生成代理（默认false）
        if (!proxyFactory.isProxyTargetClass()) {
            if (shouldProxyTargetClass(beanClass, beanName)) {
                proxyFactory.setProxyTargetClass(true);
            } else {
                // 有可代理接口，添加到ProxyFactory，没有接口，proxyFactory.setProxyTargetClass(true);
                evaluateProxyInterfaces(beanClass, proxyFactory);
            }
        }
        // 适用于该Bean的所有Advisor：从BeanFactory获取通用拦截器（默认空），加上匹配该Bean的专用拦截器，包装成Advisor返回
        Advisor[] advisors = buildAdvisors(beanName, specificInterceptors);
        proxyFactory.addAdvisors(advisors);
        proxyFactory.setTargetSource(targetSource);
        customizeProxyFactory(proxyFactory);
        // 默认false
        proxyFactory.setFrozen(this.freezeProxy);
        if (advisorsPreFiltered()) {
            proxyFactory.setPreFiltered(true);
        }
        // 创建当前Bean的代理对象
        return proxyFactory.getProxy(getProxyClassLoader());
    }

    protected boolean shouldProxyTargetClass(Class<?> beanClass, @Nullable String beanName) {
        return (this.beanFactory instanceof ConfigurableListableBeanFactory && AutoProxyUtils.shouldProxyTargetClass((ConfigurableListableBeanFactory) this.beanFactory, beanName));
    }

    protected boolean advisorsPreFiltered() {
        return false;
    }

    protected Advisor[] buildAdvisors(@Nullable String beanName, @Nullable Object[] specificInterceptors) {
        // 根据通用拦截器名称（this.interceptorNames）获取对应的Bean实例，并包装成Advisor
        Advisor[] commonInterceptors = resolveInterceptorNames();
        // 存放通用和专用
        List<Object> allInterceptors = new ArrayList<>();
        // 专用拦截器，针对当前的Bean
        if (specificInterceptors != null) {
            // 添加进去
            allInterceptors.addAll(Arrays.asList(specificInterceptors));
            if (commonInterceptors.length > 0) {
                // 哪个先哪个后
                if (this.applyCommonInterceptorsFirst) {
                    // 通用拦截器 -> 专用拦截器
                    allInterceptors.addAll(0, Arrays.asList(commonInterceptors));
                } else {
                    // 专用拦截器 -> 通用拦截器
                    allInterceptors.addAll(Arrays.asList(commonInterceptors));
                }
            }
        }
        if (logger.isTraceEnabled()) {
            int nrOfCommonInterceptors = commonInterceptors.length;
            int nrOfSpecificInterceptors = (specificInterceptors != null ? specificInterceptors.length : 0);
            logger.trace("Creating implicit proxy for bean '" + beanName + "' with " + nrOfCommonInterceptors + " common interceptors and " + nrOfSpecificInterceptors + " specific interceptors");
        }
        Advisor[] advisors = new Advisor[allInterceptors.size()];
        for (int i = 0; i < allInterceptors.size(); i++) {
            // 将上面获取的所有拦截器包装成Advisor
            advisors[i] = this.advisorAdapterRegistry.wrap(allInterceptors.get(i));
        }
        return advisors;
    }

    private Advisor[] resolveInterceptorNames() {
        BeanFactory bf = this.beanFactory;
        ConfigurableBeanFactory cbf = (bf instanceof ConfigurableBeanFactory ? (ConfigurableBeanFactory) bf : null);
        List<Advisor> advisors = new ArrayList<>();
        // 遍历拦截器Bean的名字
        for (String beanName : this.interceptorNames) {
            if (cbf == null || !cbf.isCurrentlyInCreation(beanName)) {
                Assert.state(bf != null, "BeanFactory required for resolving interceptor names");
                // 获取拦截器实例，可能是Advisor或MethodInterceptor
                Object next = bf.getBean(beanName);
                // 通过AdvisorAdapterRegistry统一包装成Advisor
                advisors.add(this.advisorAdapterRegistry.wrap(next));
            }
        }
        return advisors.toArray(new Advisor[0]);
    }

    protected void customizeProxyFactory(ProxyFactory proxyFactory) {
    }

    @Nullable
    protected abstract Object[] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String beanName, @Nullable TargetSource customTargetSource) throws BeansException;

}
