package org.springframework.boot;

import org.springframework.util.ClassUtils;

public enum WebApplicationType {

    NONE,

    SERVLET,

    REACTIVE;

    private static final String[] SERVLET_INDICATOR_CLASSES = {"javax.servlet.Servlet", "org.springframework.web.context.ConfigurableWebApplicationContext"};

    private static final String WEBMVC_INDICATOR_CLASS = "org.springframework." + "web.servlet.DispatcherServlet";

    private static final String WEBFLUX_INDICATOR_CLASS = "org." + "springframework.web.reactive.DispatcherHandler";

    private static final String JERSEY_INDICATOR_CLASS = "org.glassfish.jersey.servlet.ServletContainer";

    private static final String SERVLET_APPLICATION_CONTEXT_CLASS = "org.springframework.web.context.WebApplicationContext";

    private static final String REACTIVE_APPLICATION_CONTEXT_CLASS = "org.springframework.boot.web.reactive.context.ReactiveWebApplicationContext";

    static WebApplicationType deduceFromClasspath() {
        // 有xxx.reactive.DispatcherHandler.class && 没有xxx.servlet.DispatcherServlet && 没有xxx.jersey.servlet.ServletContainer
        if (ClassUtils.isPresent(WEBFLUX_INDICATOR_CLASS, null) && !ClassUtils.isPresent(WEBMVC_INDICATOR_CLASS, null) && !ClassUtils.isPresent(JERSEY_INDICATOR_CLASS, null)) {
            // REACTIVE
            return WebApplicationType.REACTIVE;
        }
        for (String className : SERVLET_INDICATOR_CLASSES) {
            if (!ClassUtils.isPresent(className, null)) {
                // 都没有 NONE
                return WebApplicationType.NONE;
            }
        }
        // 默认SERVLET
        return WebApplicationType.SERVLET;
    }

    static WebApplicationType deduceFromApplicationContext(Class<?> applicationContextClass) {
        if (isAssignable(SERVLET_APPLICATION_CONTEXT_CLASS, applicationContextClass)) {
            return WebApplicationType.SERVLET;
        }
        if (isAssignable(REACTIVE_APPLICATION_CONTEXT_CLASS, applicationContextClass)) {
            return WebApplicationType.REACTIVE;
        }
        return WebApplicationType.NONE;
    }

    private static boolean isAssignable(String target, Class<?> type) {
        try {
            return ClassUtils.resolveClassName(target, null).isAssignableFrom(type);
        } catch (Throwable ex) {
            return false;
        }
    }

}
