package org.springframework.web.servlet.config.annotation;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.resource.DefaultServletHttpRequestHandler;

import javax.servlet.ServletContext;
import java.util.Collections;

public class DefaultServletHandlerConfigurer {

    private final ServletContext servletContext;

    @Nullable
    private DefaultServletHttpRequestHandler handler;

    public DefaultServletHandlerConfigurer(ServletContext servletContext) {
        Assert.notNull(servletContext, "ServletContext is required");
        this.servletContext = servletContext;
    }

    public void enable() {
        enable(null);
    }

    public void enable(@Nullable String defaultServletName) {
        this.handler = new DefaultServletHttpRequestHandler();
        if (defaultServletName != null) {
            this.handler.setDefaultServletName(defaultServletName);
        }
        this.handler.setServletContext(this.servletContext);
    }

    @Nullable
    protected SimpleUrlHandlerMapping buildHandlerMapping() {
        if (this.handler == null) {
            return null;
        }
        SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
        handlerMapping.setUrlMap(Collections.singletonMap("/**", this.handler));
        handlerMapping.setOrder(Integer.MAX_VALUE);
        return handlerMapping;
    }

}
