package org.springframework.web.servlet.handler;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.util.*;

public class HandlerMappingIntrospector implements CorsConfigurationSource, ApplicationContextAware, InitializingBean {

    @Nullable
    private ApplicationContext applicationContext;

    @Nullable
    private List<HandlerMapping> handlerMappings;

    public HandlerMappingIntrospector() {
    }

    @Deprecated
    public HandlerMappingIntrospector(ApplicationContext context) {
        this.handlerMappings = initHandlerMappings(context);
    }

    public List<HandlerMapping> getHandlerMappings() {
        return (this.handlerMappings != null ? this.handlerMappings : Collections.emptyList());
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() {
        if (this.handlerMappings == null) {
            Assert.notNull(this.applicationContext, "No ApplicationContext");
            this.handlerMappings = initHandlerMappings(this.applicationContext);
        }
    }

    @Nullable
    public MatchableHandlerMapping getMatchableHandlerMapping(HttpServletRequest request) throws Exception {
        Assert.notNull(this.handlerMappings, "Handler mappings not initialized");
        HttpServletRequest wrapper = new RequestAttributeChangeIgnoringWrapper(request);
        for (HandlerMapping handlerMapping : this.handlerMappings) {
            Object handler = handlerMapping.getHandler(wrapper);
            if (handler == null) {
                continue;
            }
            if (handlerMapping instanceof MatchableHandlerMapping) {
                return ((MatchableHandlerMapping) handlerMapping);
            }
            throw new IllegalStateException("HandlerMapping is not a MatchableHandlerMapping");
        }
        return null;
    }

    @Override
    @Nullable
    public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
        Assert.notNull(this.handlerMappings, "Handler mappings not initialized");
        HttpServletRequest wrapper = new RequestAttributeChangeIgnoringWrapper(request);
        for (HandlerMapping handlerMapping : this.handlerMappings) {
            HandlerExecutionChain handler = null;
            try {
                handler = handlerMapping.getHandler(wrapper);
            } catch (Exception ex) {
                // Ignore
            }
            if (handler == null) {
                continue;
            }
            if (handler.getInterceptors() != null) {
                for (HandlerInterceptor interceptor : handler.getInterceptors()) {
                    if (interceptor instanceof CorsConfigurationSource) {
                        return ((CorsConfigurationSource) interceptor).getCorsConfiguration(wrapper);
                    }
                }
            }
            if (handler.getHandler() instanceof CorsConfigurationSource) {
                return ((CorsConfigurationSource) handler.getHandler()).getCorsConfiguration(wrapper);
            }
        }
        return null;
    }

    private static List<HandlerMapping> initHandlerMappings(ApplicationContext applicationContext) {
        Map<String, HandlerMapping> beans = BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, HandlerMapping.class, true, false);
        if (!beans.isEmpty()) {
            List<HandlerMapping> mappings = new ArrayList<>(beans.values());
            AnnotationAwareOrderComparator.sort(mappings);
            return Collections.unmodifiableList(mappings);
        }
        return Collections.unmodifiableList(initFallback(applicationContext));
    }

    private static List<HandlerMapping> initFallback(ApplicationContext applicationContext) {
        Properties props;
        String path = "DispatcherServlet.properties";
        try {
            Resource resource = new ClassPathResource(path, DispatcherServlet.class);
            props = PropertiesLoaderUtils.loadProperties(resource);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not load '" + path + "': " + ex.getMessage());
        }
        String value = props.getProperty(HandlerMapping.class.getName());
        String[] names = StringUtils.commaDelimitedListToStringArray(value);
        List<HandlerMapping> result = new ArrayList<>(names.length);
        for (String name : names) {
            try {
                Class<?> clazz = ClassUtils.forName(name, DispatcherServlet.class.getClassLoader());
                Object mapping = applicationContext.getAutowireCapableBeanFactory().createBean(clazz);
                result.add((HandlerMapping) mapping);
            } catch (ClassNotFoundException ex) {
                throw new IllegalStateException("Could not find default HandlerMapping [" + name + "]");
            }
        }
        return result;
    }

    private static class RequestAttributeChangeIgnoringWrapper extends HttpServletRequestWrapper {

        public RequestAttributeChangeIgnoringWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public void setAttribute(String name, Object value) {
            // Ignore attribute change...
        }

    }

}
