package org.springframework.web.context.support;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.HashSet;
import java.util.Set;

public class ContextExposingHttpServletRequest extends HttpServletRequestWrapper {

    private final WebApplicationContext webApplicationContext;

    @Nullable
    private final Set<String> exposedContextBeanNames;

    @Nullable
    private Set<String> explicitAttributes;

    public ContextExposingHttpServletRequest(HttpServletRequest originalRequest, WebApplicationContext context) {
        this(originalRequest, context, null);
    }

    public ContextExposingHttpServletRequest(HttpServletRequest originalRequest, WebApplicationContext context, @Nullable Set<String> exposedContextBeanNames) {
        super(originalRequest);
        Assert.notNull(context, "WebApplicationContext must not be null");
        this.webApplicationContext = context;
        this.exposedContextBeanNames = exposedContextBeanNames;
    }

    public final WebApplicationContext getWebApplicationContext() {
        return this.webApplicationContext;
    }

    @Override
    @Nullable
    public Object getAttribute(String name) {
        if ((this.explicitAttributes == null || !this.explicitAttributes.contains(name)) && (this.exposedContextBeanNames == null || this.exposedContextBeanNames.contains(name)) && this.webApplicationContext.containsBean(name)) {
            return this.webApplicationContext.getBean(name);
        } else {
            return super.getAttribute(name);
        }
    }

    @Override
    public void setAttribute(String name, Object value) {
        super.setAttribute(name, value);
        if (this.explicitAttributes == null) {
            this.explicitAttributes = new HashSet<>(8);
        }
        this.explicitAttributes.add(name);
    }

}
