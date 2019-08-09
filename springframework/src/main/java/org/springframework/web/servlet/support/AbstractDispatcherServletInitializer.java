package org.springframework.web.servlet.support;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.core.Conventions;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.AbstractContextLoaderInitializer;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FrameworkServlet;

import javax.servlet.*;
import javax.servlet.FilterRegistration.Dynamic;
import java.util.EnumSet;

public abstract class AbstractDispatcherServletInitializer extends AbstractContextLoaderInitializer {

    public static final String DEFAULT_SERVLET_NAME = "dispatcher";

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        super.onStartup(servletContext);
        registerDispatcherServlet(servletContext);
    }

    protected void registerDispatcherServlet(ServletContext servletContext) {
        String servletName = getServletName();
        Assert.hasLength(servletName, "getServletName() must not return null or empty");
        WebApplicationContext servletAppContext = createServletApplicationContext();
        Assert.notNull(servletAppContext, "createServletApplicationContext() must not return null");
        FrameworkServlet dispatcherServlet = createDispatcherServlet(servletAppContext);
        Assert.notNull(dispatcherServlet, "createDispatcherServlet(WebApplicationContext) must not return null");
        dispatcherServlet.setContextInitializers(getServletApplicationContextInitializers());
        ServletRegistration.Dynamic registration = servletContext.addServlet(servletName, dispatcherServlet);
        if (registration == null) {
            throw new IllegalStateException("Failed to register servlet with name '" + servletName + "'. " + "Check if there is another servlet registered under the same name.");
        }
        registration.setLoadOnStartup(1);
        registration.addMapping(getServletMappings());
        registration.setAsyncSupported(isAsyncSupported());
        Filter[] filters = getServletFilters();
        if (!ObjectUtils.isEmpty(filters)) {
            for (Filter filter : filters) {
                registerServletFilter(servletContext, filter);
            }
        }
        customizeRegistration(registration);
    }

    protected String getServletName() {
        return DEFAULT_SERVLET_NAME;
    }

    protected abstract WebApplicationContext createServletApplicationContext();

    protected FrameworkServlet createDispatcherServlet(WebApplicationContext servletAppContext) {
        return new DispatcherServlet(servletAppContext);
    }

    @Nullable
    protected ApplicationContextInitializer<?>[] getServletApplicationContextInitializers() {
        return null;
    }

    protected abstract String[] getServletMappings();

    @Nullable
    protected Filter[] getServletFilters() {
        return null;
    }

    protected FilterRegistration.Dynamic registerServletFilter(ServletContext servletContext, Filter filter) {
        String filterName = Conventions.getVariableName(filter);
        Dynamic registration = servletContext.addFilter(filterName, filter);
        if (registration == null) {
            int counter = 0;
            while (registration == null) {
                if (counter == 100) {
                    throw new IllegalStateException("Failed to register filter with name '" + filterName + "'. " + "Check if there is another filter registered under the same name.");
                }
                registration = servletContext.addFilter(filterName + "#" + counter, filter);
                counter++;
            }
        }
        registration.setAsyncSupported(isAsyncSupported());
        registration.addMappingForServletNames(getDispatcherTypes(), false, getServletName());
        return registration;
    }

    private EnumSet<DispatcherType> getDispatcherTypes() {
        return (isAsyncSupported() ? EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.INCLUDE, DispatcherType.ASYNC) : EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.INCLUDE));
    }

    protected boolean isAsyncSupported() {
        return true;
    }

    protected void customizeRegistration(ServletRegistration.Dynamic registration) {
    }

}
