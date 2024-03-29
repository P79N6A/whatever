package org.springframework.web.servlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.*;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.ServletContextResourceLoader;
import org.springframework.web.context.support.StandardServletEnvironment;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("serial")
public abstract class HttpServletBean extends HttpServlet implements EnvironmentCapable, EnvironmentAware {

    protected final Log logger = LogFactory.getLog(getClass());

    @Nullable
    private ConfigurableEnvironment environment;

    private final Set<String> requiredProperties = new HashSet<>(4);

    protected final void addRequiredProperty(String property) {
        this.requiredProperties.add(property);
    }

    @Override
    public void setEnvironment(Environment environment) {
        Assert.isInstanceOf(ConfigurableEnvironment.class, environment, "ConfigurableEnvironment required");
        this.environment = (ConfigurableEnvironment) environment;
    }

    @Override
    public ConfigurableEnvironment getEnvironment() {
        if (this.environment == null) {
            this.environment = createEnvironment();
        }
        return this.environment;
    }

    protected ConfigurableEnvironment createEnvironment() {
        return new StandardServletEnvironment();
    }

    @Override
    public final void init() throws ServletException {
        // Set bean properties from init parameters.
        PropertyValues pvs = new ServletConfigPropertyValues(getServletConfig(), this.requiredProperties);
        // 默认空
        if (!pvs.isEmpty()) {
            try {
                BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(this);
                ResourceLoader resourceLoader = new ServletContextResourceLoader(getServletContext());
                bw.registerCustomEditor(Resource.class, new ResourceEditor(resourceLoader, getEnvironment()));
                initBeanWrapper(bw);
                bw.setPropertyValues(pvs, true);
            } catch (BeansException ex) {
                if (logger.isErrorEnabled()) {
                    logger.error("Failed to set bean properties on servlet '" + getServletName() + "'", ex);
                }
                throw ex;
            }
        }
        // Let subclasses do whatever initialization they like.
        initServletBean();
    }

    protected void initBeanWrapper(BeanWrapper bw) throws BeansException {
    }

    protected void initServletBean() throws ServletException {
    }

    @Override
    @Nullable
    public String getServletName() {
        return (getServletConfig() != null ? getServletConfig().getServletName() : null);
    }

    private static class ServletConfigPropertyValues extends MutablePropertyValues {

        public ServletConfigPropertyValues(ServletConfig config, Set<String> requiredProperties) throws ServletException {
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
                throw new ServletException("Initialization from ServletConfig for servlet '" + config.getServletName() + "' failed; the following required properties were missing: " + StringUtils.collectionToDelimitedString(missingProps, ", "));
            }
        }

    }

}
