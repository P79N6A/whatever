package org.springframework.web.servlet.config.annotation;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.ServletContext;
import java.util.*;

public class ResourceHandlerRegistry {

    private final ServletContext servletContext;

    private final ApplicationContext applicationContext;

    @Nullable
    private final ContentNegotiationManager contentNegotiationManager;

    @Nullable
    private final UrlPathHelper pathHelper;

    private final List<ResourceHandlerRegistration> registrations = new ArrayList<>();

    private int order = Ordered.LOWEST_PRECEDENCE - 1;

    public ResourceHandlerRegistry(ApplicationContext applicationContext, ServletContext servletContext) {
        this(applicationContext, servletContext, null);
    }

    public ResourceHandlerRegistry(ApplicationContext applicationContext, ServletContext servletContext, @Nullable ContentNegotiationManager contentNegotiationManager) {
        this(applicationContext, servletContext, contentNegotiationManager, null);
    }

    public ResourceHandlerRegistry(ApplicationContext applicationContext, ServletContext servletContext, @Nullable ContentNegotiationManager contentNegotiationManager, @Nullable UrlPathHelper pathHelper) {
        Assert.notNull(applicationContext, "ApplicationContext is required");
        this.applicationContext = applicationContext;
        this.servletContext = servletContext;
        this.contentNegotiationManager = contentNegotiationManager;
        this.pathHelper = pathHelper;
    }

    public ResourceHandlerRegistration addResourceHandler(String... pathPatterns) {
        ResourceHandlerRegistration registration = new ResourceHandlerRegistration(pathPatterns);
        this.registrations.add(registration);
        return registration;
    }

    public boolean hasMappingForPattern(String pathPattern) {
        for (ResourceHandlerRegistration registration : this.registrations) {
            if (Arrays.asList(registration.getPathPatterns()).contains(pathPattern)) {
                return true;
            }
        }
        return false;
    }

    public ResourceHandlerRegistry setOrder(int order) {
        this.order = order;
        return this;
    }

    @Nullable
    protected AbstractHandlerMapping getHandlerMapping() {
        if (this.registrations.isEmpty()) {
            return null;
        }
        Map<String, HttpRequestHandler> urlMap = new LinkedHashMap<>();
        for (ResourceHandlerRegistration registration : this.registrations) {
            for (String pathPattern : registration.getPathPatterns()) {
                ResourceHttpRequestHandler handler = registration.getRequestHandler();
                if (this.pathHelper != null) {
                    handler.setUrlPathHelper(this.pathHelper);
                }
                if (this.contentNegotiationManager != null) {
                    handler.setContentNegotiationManager(this.contentNegotiationManager);
                }
                handler.setServletContext(this.servletContext);
                handler.setApplicationContext(this.applicationContext);
                try {
                    handler.afterPropertiesSet();
                } catch (Throwable ex) {
                    throw new BeanInitializationException("Failed to init ResourceHttpRequestHandler", ex);
                }
                urlMap.put(pathPattern, handler);
            }
        }
        SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
        handlerMapping.setOrder(this.order);
        handlerMapping.setUrlMap(urlMap);
        return handlerMapping;
    }

}
