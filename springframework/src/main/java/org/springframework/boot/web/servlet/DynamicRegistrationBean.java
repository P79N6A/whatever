package org.springframework.boot.web.servlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.Conventions;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.servlet.Registration;
import javax.servlet.ServletContext;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class DynamicRegistrationBean<D extends Registration.Dynamic> extends RegistrationBean {

    private static final Log logger = LogFactory.getLog(RegistrationBean.class);

    private String name;

    private boolean asyncSupported = true;

    private Map<String, String> initParameters = new LinkedHashMap<>();

    public void setName(String name) {
        Assert.hasLength(name, "Name must not be empty");
        this.name = name;
    }

    public void setAsyncSupported(boolean asyncSupported) {
        this.asyncSupported = asyncSupported;
    }

    public boolean isAsyncSupported() {
        return this.asyncSupported;
    }

    public void setInitParameters(Map<String, String> initParameters) {
        Assert.notNull(initParameters, "InitParameters must not be null");
        this.initParameters = new LinkedHashMap<>(initParameters);
    }

    public Map<String, String> getInitParameters() {
        return this.initParameters;
    }

    public void addInitParameter(String name, String value) {
        Assert.notNull(name, "Name must not be null");
        this.initParameters.put(name, value);
    }

    @Override
    protected final void register(String description, ServletContext servletContext) {
        // 将DispatcherServlet加入ServletContext（DispatcherServlet继承了HttpServlet）
        D registration = addRegistration(description, servletContext);
        if (registration == null) {
            logger.info(StringUtils.capitalize(description) + " was not registered " + "(possibly already registered?)");
            return;
        }
        configure(registration);
    }

    protected abstract D addRegistration(String description, ServletContext servletContext);

    protected void configure(D registration) {
        // 异步支持，默认true
        registration.setAsyncSupported(this.asyncSupported);
        if (!this.initParameters.isEmpty()) {
            // 初始化参数
            registration.setInitParameters(this.initParameters);
        }
    }

    protected final String getOrDeduceName(Object value) {
        return (this.name != null) ? this.name : Conventions.getVariableName(value);
    }

}
