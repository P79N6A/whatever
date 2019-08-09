package org.springframework.web.servlet.handler;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.lang.Nullable;
import org.springframework.web.context.ServletConfigAware;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.Collections;
import java.util.Enumeration;

public class SimpleServletPostProcessor implements DestructionAwareBeanPostProcessor, ServletContextAware, ServletConfigAware {

    private boolean useSharedServletConfig = true;

    @Nullable
    private ServletContext servletContext;

    @Nullable
    private ServletConfig servletConfig;

    public void setUseSharedServletConfig(boolean useSharedServletConfig) {
        this.useSharedServletConfig = useSharedServletConfig;
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @Override
    public void setServletConfig(ServletConfig servletConfig) {
        this.servletConfig = servletConfig;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof Servlet) {
            ServletConfig config = this.servletConfig;
            if (config == null || !this.useSharedServletConfig) {
                config = new DelegatingServletConfig(beanName, this.servletContext);
            }
            try {
                ((Servlet) bean).init(config);
            } catch (ServletException ex) {
                throw new BeanInitializationException("Servlet.init threw exception", ex);
            }
        }
        return bean;
    }

    @Override
    public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
        if (bean instanceof Servlet) {
            ((Servlet) bean).destroy();
        }
    }

    @Override
    public boolean requiresDestruction(Object bean) {
        return (bean instanceof Servlet);
    }

    private static class DelegatingServletConfig implements ServletConfig {

        private final String servletName;

        @Nullable
        private final ServletContext servletContext;

        public DelegatingServletConfig(String servletName, @Nullable ServletContext servletContext) {
            this.servletName = servletName;
            this.servletContext = servletContext;
        }

        @Override
        public String getServletName() {
            return this.servletName;
        }

        @Override
        @Nullable
        public ServletContext getServletContext() {
            return this.servletContext;
        }

        @Override
        @Nullable
        public String getInitParameter(String paramName) {
            return null;
        }

        @Override
        public Enumeration<String> getInitParameterNames() {
            return Collections.enumeration(Collections.emptySet());
        }

    }

}
