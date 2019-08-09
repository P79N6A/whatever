package org.springframework.web.context.support;

import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import javax.servlet.ServletContext;

public class ServletContextPropertySource extends EnumerablePropertySource<ServletContext> {

    public ServletContextPropertySource(String name, ServletContext servletContext) {
        super(name, servletContext);
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
