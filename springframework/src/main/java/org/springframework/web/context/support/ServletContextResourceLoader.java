package org.springframework.web.context.support;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import javax.servlet.ServletContext;

public class ServletContextResourceLoader extends DefaultResourceLoader {

    private final ServletContext servletContext;

    public ServletContextResourceLoader(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @Override
    protected Resource getResourceByPath(String path) {
        return new ServletContextResource(this.servletContext, path);
    }

}
