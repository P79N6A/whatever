package org.springframework.web.context.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.lang.Nullable;
import org.springframework.web.context.ServletConfigAware;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

public class ServletContextAwareProcessor implements BeanPostProcessor {

    @Nullable
    private ServletContext servletContext;

    @Nullable
    private ServletConfig servletConfig;

    protected ServletContextAwareProcessor() {
    }

    public ServletContextAwareProcessor(ServletContext servletContext) {
        this(servletContext, null);
    }

    public ServletContextAwareProcessor(ServletConfig servletConfig) {
        this(null, servletConfig);
    }

    public ServletContextAwareProcessor(@Nullable ServletContext servletContext, @Nullable ServletConfig servletConfig) {
        this.servletContext = servletContext;
        this.servletConfig = servletConfig;
    }

    @Nullable
    protected ServletContext getServletContext() {
        if (this.servletContext == null && getServletConfig() != null) {
            return getServletConfig().getServletContext();
        }
        return this.servletContext;
    }

    @Nullable
    protected ServletConfig getServletConfig() {
        return this.servletConfig;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (getServletContext() != null && bean instanceof ServletContextAware) {
            ((ServletContextAware) bean).setServletContext(getServletContext());
        }
        if (getServletConfig() != null && bean instanceof ServletConfigAware) {
            ((ServletConfigAware) bean).setServletConfig(getServletConfig());
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;
    }

}
