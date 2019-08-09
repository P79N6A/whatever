package org.springframework.web.filter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.*;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.support.ServletContextResourceLoader;
import org.springframework.web.context.support.StandardServletEnvironment;
import org.springframework.web.util.NestedServletException;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

public abstract class GenericFilterBean implements Filter, BeanNameAware, EnvironmentAware, EnvironmentCapable, ServletContextAware, InitializingBean, DisposableBean {

    protected final Log logger = LogFactory.getLog(getClass());

    @Nullable
    private String beanName;

    @Nullable
    private Environment environment;

    @Nullable
    private ServletContext servletContext;

    @Nullable
    private FilterConfig filterConfig;

    private final Set<String> requiredProperties = new HashSet<>(4);

    @Override
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public Environment getEnvironment() {
        if (this.environment == null) {
            this.environment = createEnvironment();
        }
        return this.environment;
    }

    protected Environment createEnvironment() {
        return new StandardServletEnvironment();
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @Override
    public void afterPropertiesSet() throws ServletException {
        initFilterBean();
    }

    @Override
    public void destroy() {
    }

    protected final void addRequiredProperty(String property) {
        this.requiredProperties.add(property);
    }

    @Override
    public final void init(FilterConfig filterConfig) throws ServletException {
        Assert.notNull(filterConfig, "FilterConfig must not be null");
        this.filterConfig = filterConfig;
        // Set bean properties from init parameters.
        PropertyValues pvs = new FilterConfigPropertyValues(filterConfig, this.requiredProperties);
        if (!pvs.isEmpty()) {
            try {
                BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(this);
                ResourceLoader resourceLoader = new ServletContextResourceLoader(filterConfig.getServletContext());
                Environment env = this.environment;
                if (env == null) {
                    env = new StandardServletEnvironment();
                }
                bw.registerCustomEditor(Resource.class, new ResourceEditor(resourceLoader, env));
                initBeanWrapper(bw);
                bw.setPropertyValues(pvs, true);
            } catch (BeansException ex) {
                String msg = "Failed to set bean properties on filter '" + filterConfig.getFilterName() + "': " + ex.getMessage();
                logger.error(msg, ex);
                throw new NestedServletException(msg, ex);
            }
        }
        // Let subclasses do whatever initialization they like.
        initFilterBean();
        if (logger.isDebugEnabled()) {
            logger.debug("Filter '" + filterConfig.getFilterName() + "' configured for use");
        }
    }

    protected void initBeanWrapper(BeanWrapper bw) throws BeansException {
    }

    protected void initFilterBean() throws ServletException {
    }

    @Nullable
    public FilterConfig getFilterConfig() {
        return this.filterConfig;
    }

    @Nullable
    protected String getFilterName() {
        return (this.filterConfig != null ? this.filterConfig.getFilterName() : this.beanName);
    }

    protected ServletContext getServletContext() {
        if (this.filterConfig != null) {
            return this.filterConfig.getServletContext();
        } else if (this.servletContext != null) {
            return this.servletContext;
        } else {
            throw new IllegalStateException("No ServletContext");
        }
    }

    @SuppressWarnings("serial")
    private static class FilterConfigPropertyValues extends MutablePropertyValues {

        public FilterConfigPropertyValues(FilterConfig config, Set<String> requiredProperties) throws ServletException {
            Set<String> missingProps = (!CollectionUtils.isEmpty(requiredProperties) ? new HashSet<>(requiredProperties) : null);
            Enumeration<String> paramNames = config.getInitParameterNames();
            while (paramNames.hasMoreElements()) {
                String property = paramNames.nextElement();
                Object value = config.getInitParameter(property);
                addPropertyValue(new PropertyValue(property, value));
                if (missingProps != null) {
                    missingProps.remove(property);
                }
            }
            // Fail if we are still missing properties.
            if (!CollectionUtils.isEmpty(missingProps)) {
                throw new ServletException("Initialization from FilterConfig for filter '" + config.getFilterName() + "' failed; the following required properties were missing: " + StringUtils.collectionToDelimitedString(missingProps, ", "));
            }
        }

    }

}
