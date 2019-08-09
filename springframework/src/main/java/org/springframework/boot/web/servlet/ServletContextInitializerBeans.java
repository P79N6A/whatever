package org.springframework.boot.web.servlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.servlet.Filter;
import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * ServletContextInitializer实例的集合
 */
public class ServletContextInitializerBeans extends AbstractCollection<ServletContextInitializer> {

    private static final String DISPATCHER_SERVLET_NAME = "dispatcherServlet";

    private static final Log logger = LogFactory.getLog(ServletContextInitializerBeans.class);

    /**
     * 避免重复处理
     */
    private final Set<Object> seen = new HashSet<>();

    /**
     * Map<K, List<V>>
     */
    private final MultiValueMap<Class<?>, ServletContextInitializer> initializers;

    private final List<Class<? extends ServletContextInitializer>> initializerTypes;

    /**
     * 排序后的实例
     */
    private List<ServletContextInitializer> sortedList;

    @SafeVarargs
    public ServletContextInitializerBeans(ListableBeanFactory beanFactory, Class<? extends ServletContextInitializer>... initializerTypes) {
        this.initializers = new LinkedMultiValueMap<>();
        this.initializerTypes = (initializerTypes.length != 0) ? Arrays.asList(initializerTypes) : Collections.singletonList(ServletContextInitializer.class);
        // 从BeanFactory实例化ServletContextInitializer类型的Bean，这里是DispatcherServletRegistrationBean，并加入加入initializers集合
        addServletContextInitializerBeans(beanFactory);
        // 从BeanFactory实例化Servlet类型的Bean，通过RegistrationBeanAdapter生成对应的RegistrationBean，并将它加入initializers集合
        // 从BeanFactory实例化Filter类型的Bean，通过RegistrationBeanAdapter生成对应的RegistrationBean，并将它加入initializers集合
        // 从BeanFactory实例化各种EventListener类型的Bean，通过RegistrationBeanAdapter生成对应的RegistrationBean，并将它加入initializers集合
        addAdaptableBeans(beanFactory);
        // 排序
        List<ServletContextInitializer> sortedInitializers = this.initializers.values().stream().flatMap((value) -> value.stream().sorted(AnnotationAwareOrderComparator.INSTANCE)).collect(Collectors.toList());
        this.sortedList = Collections.unmodifiableList(sortedInitializers);
        // 日志
        logMappings(this.initializers);
    }

    private void addServletContextInitializerBeans(ListableBeanFactory beanFactory) {
        for (Class<? extends ServletContextInitializer> initializerType : this.initializerTypes) {
            // 实例化DispatcherServletRegistrationBean
            for (Entry<String, ? extends ServletContextInitializer> initializerBean : getOrderedBeansOfType(beanFactory, initializerType)) {
                // 加入initializers集合
                addServletContextInitializerBean(initializerBean.getKey(), initializerBean.getValue(), beanFactory);
            }
        }
    }

    private void addServletContextInitializerBean(String beanName, ServletContextInitializer initializer, ListableBeanFactory beanFactory) {
        // DispatcherServletRegistrationBean
        if (initializer instanceof ServletRegistrationBean) {
            Servlet source = ((ServletRegistrationBean<?>) initializer).getServlet();
            addServletContextInitializerBean(Servlet.class, beanName, initializer, beanFactory, source);
        } else if (initializer instanceof FilterRegistrationBean) {
            Filter source = ((FilterRegistrationBean<?>) initializer).getFilter();
            addServletContextInitializerBean(Filter.class, beanName, initializer, beanFactory, source);
        } else if (initializer instanceof DelegatingFilterProxyRegistrationBean) {
            String source = ((DelegatingFilterProxyRegistrationBean) initializer).getTargetBeanName();
            addServletContextInitializerBean(Filter.class, beanName, initializer, beanFactory, source);
        } else if (initializer instanceof ServletListenerRegistrationBean) {
            EventListener source = ((ServletListenerRegistrationBean<?>) initializer).getListener();
            addServletContextInitializerBean(EventListener.class, beanName, initializer, beanFactory, source);
        } else {
            addServletContextInitializerBean(ServletContextInitializer.class, beanName, initializer, beanFactory, initializer);
        }
    }

    private void addServletContextInitializerBean(Class<?> type, String beanName, ServletContextInitializer initializer, ListableBeanFactory beanFactory, Object source) {
        // 添加
        this.initializers.add(type, initializer);
        if (source != null) {
            // Mark the underlying source as seen in case it wraps an existing bean
            this.seen.add(source);
        }
        if (logger.isTraceEnabled()) {
            String resourceDescription = getResourceDescription(beanName, beanFactory);
            int order = getOrder(initializer);
            logger.trace("Added existing " + type.getSimpleName() + " initializer bean '" + beanName + "'; order=" + order + ", resource=" + resourceDescription);
        }
    }

    private String getResourceDescription(String beanName, ListableBeanFactory beanFactory) {
        if (beanFactory instanceof BeanDefinitionRegistry) {
            BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
            return registry.getBeanDefinition(beanName).getResourceDescription();
        }
        return "unknown";
    }

    @SuppressWarnings("unchecked")
    protected void addAdaptableBeans(ListableBeanFactory beanFactory) {
        MultipartConfigElement multipartConfig = getMultipartConfig(beanFactory);
        // 默认0个，DispatcherServlet已被addServletContextInitializerBeans处理
        addAsRegistrationBean(beanFactory, Servlet.class, new ServletRegistrationBeanAdapter(multipartConfig));
        // 默认4个，CharacterEncodingFilter，HiddenHttpMethodFilter，HttpPutFormContentFilter，RequestContextFilter
        addAsRegistrationBean(beanFactory, Filter.class, new FilterRegistrationBeanAdapter());
        for (Class<?> listenerType : ServletListenerRegistrationBean.getSupportedTypes()) {
            addAsRegistrationBean(beanFactory, EventListener.class, (Class<EventListener>) listenerType, new ServletListenerRegistrationBeanAdapter());
        }
    }

    private MultipartConfigElement getMultipartConfig(ListableBeanFactory beanFactory) {
        // MultipartConfigElement
        List<Entry<String, MultipartConfigElement>> beans = getOrderedBeansOfType(beanFactory, MultipartConfigElement.class);
        // 返回第一个
        return beans.isEmpty() ? null : beans.get(0).getValue();
    }

    protected <T> void addAsRegistrationBean(ListableBeanFactory beanFactory, Class<T> type, RegistrationBeanAdapter<T> adapter) {
        addAsRegistrationBean(beanFactory, type, type, adapter);
    }

    private <T, B extends T> void addAsRegistrationBean(ListableBeanFactory beanFactory, Class<T> type, Class<B> beanType, RegistrationBeanAdapter<T> adapter) {
        // 实例化对应beanType类型的Bean
        List<Map.Entry<String, B>> entries = getOrderedBeansOfType(beanFactory, beanType, this.seen);
        for (Entry<String, B> entry : entries) {
            String beanName = entry.getKey();
            B bean = entry.getValue();
            // 之前没加过
            if (this.seen.add(bean)) {
                // 通过RegistrationBeanAdapter生成对应的RegistrationBean
                RegistrationBean registration = adapter.createRegistrationBean(beanName, bean, entries.size());
                int order = getOrder(bean);
                registration.setOrder(order);
                this.initializers.add(type, registration);
                if (logger.isTraceEnabled()) {
                    logger.trace("Created " + type.getSimpleName() + " initializer for bean '" + beanName + "'; order=" + order + ", resource=" + getResourceDescription(beanName, beanFactory));
                }
            }
        }
    }

    private int getOrder(Object value) {
        return new AnnotationAwareOrderComparator() {
            @Override
            public int getOrder(Object obj) {
                return super.getOrder(obj);
            }
        }.getOrder(value);
    }

    private <T> List<Entry<String, T>> getOrderedBeansOfType(ListableBeanFactory beanFactory, Class<T> type) {
        return getOrderedBeansOfType(beanFactory, type, Collections.emptySet());
    }

    private <T> List<Entry<String, T>> getOrderedBeansOfType(ListableBeanFactory beanFactory, Class<T> type, Set<?> excludes) {
        String[] names = beanFactory.getBeanNamesForType(type, true, false);
        Map<String, T> map = new LinkedHashMap<>();
        for (String name : names) {
            if (!excludes.contains(name) && !ScopedProxyUtils.isScopedTarget(name)) {
                T bean = beanFactory.getBean(name, type);
                if (!excludes.contains(bean)) {
                    map.put(name, bean);
                }
            }
        }
        List<Entry<String, T>> beans = new ArrayList<>(map.entrySet());
        beans.sort((o1, o2) -> AnnotationAwareOrderComparator.INSTANCE.compare(o1.getValue(), o2.getValue()));
        return beans;
    }

    private void logMappings(MultiValueMap<Class<?>, ServletContextInitializer> initializers) {
        if (logger.isDebugEnabled()) {
            logMappings("filters", initializers, Filter.class, FilterRegistrationBean.class);
            logMappings("servlets", initializers, Servlet.class, ServletRegistrationBean.class);
        }
    }

    private void logMappings(String name, MultiValueMap<Class<?>, ServletContextInitializer> initializers, Class<?> type, Class<? extends RegistrationBean> registrationType) {
        List<ServletContextInitializer> registrations = new ArrayList<>();
        registrations.addAll(initializers.getOrDefault(registrationType, Collections.emptyList()));
        registrations.addAll(initializers.getOrDefault(type, Collections.emptyList()));
        String info = registrations.stream().map(Object::toString).collect(Collectors.joining(", "));
        logger.debug("Mapping " + name + ": " + info);
    }

    @Override
    public Iterator<ServletContextInitializer> iterator() {
        return this.sortedList.iterator();
    }

    @Override
    public int size() {
        return this.sortedList.size();
    }

    @FunctionalInterface
    protected interface RegistrationBeanAdapter<T> {

        RegistrationBean createRegistrationBean(String name, T source, int totalNumberOfSourceBeans);

    }

    private static class ServletRegistrationBeanAdapter implements RegistrationBeanAdapter<Servlet> {

        private final MultipartConfigElement multipartConfig;

        ServletRegistrationBeanAdapter(MultipartConfigElement multipartConfig) {
            this.multipartConfig = multipartConfig;
        }

        @Override
        public RegistrationBean createRegistrationBean(String name, Servlet source, int totalNumberOfSourceBeans) {
            String url = (totalNumberOfSourceBeans != 1) ? "/" + name + "/" : "/";
            if (name.equals(DISPATCHER_SERVLET_NAME)) {
                url = "/"; // always map the main dispatcherServlet to "/"
            }
            ServletRegistrationBean<Servlet> bean = new ServletRegistrationBean<>(source, url);
            bean.setName(name);
            bean.setMultipartConfig(this.multipartConfig);
            return bean;
        }

    }

    private static class FilterRegistrationBeanAdapter implements RegistrationBeanAdapter<Filter> {

        @Override
        public RegistrationBean createRegistrationBean(String name, Filter source, int totalNumberOfSourceBeans) {
            FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>(source);
            bean.setName(name);
            return bean;
        }

    }

    private static class ServletListenerRegistrationBeanAdapter implements RegistrationBeanAdapter<EventListener> {

        @Override
        public RegistrationBean createRegistrationBean(String name, EventListener source, int totalNumberOfSourceBeans) {
            return new ServletListenerRegistrationBean<>(source);
        }

    }

}
