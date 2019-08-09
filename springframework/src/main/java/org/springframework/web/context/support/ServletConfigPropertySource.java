package org.springframework.web.context.support;

import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import javax.servlet.ServletConfig;

public class ServletConfigPropertySource extends EnumerablePropertySource<ServletConfig> {

    public ServletConfigPropertySource(String name, ServletConfig servletConfig) {
        super(name, servletConfig);
    }

    @Override
    public String[] getPropertyNames() {
        return StringUtils.toStringArray(this.source.getInitParameterNames());
    }

    @Override
    @Nullable
    public String getProperty(String name) {
        return this.source.getInitParameter(name);
    }

}
