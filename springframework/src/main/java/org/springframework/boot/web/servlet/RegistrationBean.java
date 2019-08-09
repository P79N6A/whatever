package org.springframework.boot.web.servlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

public abstract class RegistrationBean implements ServletContextInitializer, Ordered {

    private static final Log logger = LogFactory.getLog(RegistrationBean.class);

    private int order = Ordered.LOWEST_PRECEDENCE;

    private boolean enabled = true;

    @Override
    public final void onStartup(ServletContext servletContext) throws ServletException {
        String description = getDescription();
        if (!isEnabled()) {
            logger.info(StringUtils.capitalize(description) + " was not registered (disabled)");
            return;
        }
        register(description, servletContext);
    }

    protected abstract String getDescription();

    protected abstract void register(String description, ServletContext servletContext);

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public int getOrder() {
        return this.order;
    }

}
