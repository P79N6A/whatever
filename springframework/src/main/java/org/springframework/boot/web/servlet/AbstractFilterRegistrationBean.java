package org.springframework.boot.web.servlet;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.ServletContext;
import java.util.*;

public abstract class AbstractFilterRegistrationBean<T extends Filter> extends DynamicRegistrationBean<Dynamic> {

    private static final String[] DEFAULT_URL_MAPPINGS = {"/*"};

    private Set<ServletRegistrationBean<?>> servletRegistrationBeans = new LinkedHashSet<>();

    private Set<String> servletNames = new LinkedHashSet<>();

    private Set<String> urlPatterns = new LinkedHashSet<>();

    private EnumSet<DispatcherType> dispatcherTypes;

    private boolean matchAfter = false;

    AbstractFilterRegistrationBean(ServletRegistrationBean<?>... servletRegistrationBeans) {
        Assert.notNull(servletRegistrationBeans, "ServletRegistrationBeans must not be null");
        Collections.addAll(this.servletRegistrationBeans, servletRegistrationBeans);
    }

    public void setServletRegistrationBeans(Collection<? extends ServletRegistrationBean<?>> servletRegistrationBeans) {
        Assert.notNull(servletRegistrationBeans, "ServletRegistrationBeans must not be null");
        this.servletRegistrationBeans = new LinkedHashSet<>(servletRegistrationBeans);
    }

    public Collection<ServletRegistrationBean<?>> getServletRegistrationBeans() {
        return this.servletRegistrationBeans;
    }

    public void addServletRegistrationBeans(ServletRegistrationBean<?>... servletRegistrationBeans) {
        Assert.notNull(servletRegistrationBeans, "ServletRegistrationBeans must not be null");
        Collections.addAll(this.servletRegistrationBeans, servletRegistrationBeans);
    }

    public void setServletNames(Collection<String> servletNames) {
        Assert.notNull(servletNames, "ServletNames must not be null");
        this.servletNames = new LinkedHashSet<>(servletNames);
    }

    public Collection<String> getServletNames() {
        return this.servletNames;
    }

    public void addServletNames(String... servletNames) {
        Assert.notNull(servletNames, "ServletNames must not be null");
        this.servletNames.addAll(Arrays.asList(servletNames));
    }

    public void setUrlPatterns(Collection<String> urlPatterns) {
        Assert.notNull(urlPatterns, "UrlPatterns must not be null");
        this.urlPatterns = new LinkedHashSet<>(urlPatterns);
    }

    public Collection<String> getUrlPatterns() {
        return this.urlPatterns;
    }

    public void addUrlPatterns(String... urlPatterns) {
        Assert.notNull(urlPatterns, "UrlPatterns must not be null");
        Collections.addAll(this.urlPatterns, urlPatterns);
    }

    public void setDispatcherTypes(DispatcherType first, DispatcherType... rest) {
        this.dispatcherTypes = EnumSet.of(first, rest);
    }

    public void setDispatcherTypes(EnumSet<DispatcherType> dispatcherTypes) {
        this.dispatcherTypes = dispatcherTypes;
    }

    public void setMatchAfter(boolean matchAfter) {
        this.matchAfter = matchAfter;
    }

    public boolean isMatchAfter() {
        return this.matchAfter;
    }

    @Override
    protected String getDescription() {
        Filter filter = getFilter();
        Assert.notNull(filter, "Filter must not be null");
        return "filter " + getOrDeduceName(filter);
    }

    @Override
    protected Dynamic addRegistration(String description, ServletContext servletContext) {
        Filter filter = getFilter();
        return servletContext.addFilter(getOrDeduceName(filter), filter);
    }

    @Override
    protected void configure(FilterRegistration.Dynamic registration) {
        super.configure(registration);
        EnumSet<DispatcherType> dispatcherTypes = this.dispatcherTypes;
        if (dispatcherTypes == null) {
            dispatcherTypes = EnumSet.of(DispatcherType.REQUEST);
        }
        Set<String> servletNames = new LinkedHashSet<>();
        for (ServletRegistrationBean<?> servletRegistrationBean : this.servletRegistrationBeans) {
            servletNames.add(servletRegistrationBean.getServletName());
        }
        servletNames.addAll(this.servletNames);
        if (servletNames.isEmpty() && this.urlPatterns.isEmpty()) {
            registration.addMappingForUrlPatterns(dispatcherTypes, this.matchAfter, DEFAULT_URL_MAPPINGS);
        } else {
            if (!servletNames.isEmpty()) {
                registration.addMappingForServletNames(dispatcherTypes, this.matchAfter, StringUtils.toStringArray(servletNames));
            }
            if (!this.urlPatterns.isEmpty()) {
                registration.addMappingForUrlPatterns(dispatcherTypes, this.matchAfter, StringUtils.toStringArray(this.urlPatterns));
            }
        }
    }

    public abstract T getFilter();

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(getOrDeduceName(this));
        if (this.servletNames.isEmpty() && this.urlPatterns.isEmpty()) {
            builder.append(" urls=").append(Arrays.toString(DEFAULT_URL_MAPPINGS));
        } else {
            if (!this.servletNames.isEmpty()) {
                builder.append(" servlets=").append(this.servletNames);
            }
            if (!this.urlPatterns.isEmpty()) {
                builder.append(" urls=").append(this.urlPatterns);
            }
        }
        return builder.toString();
    }

}
