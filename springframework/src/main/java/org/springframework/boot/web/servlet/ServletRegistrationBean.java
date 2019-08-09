package org.springframework.boot.web.servlet;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class ServletRegistrationBean<T extends Servlet> extends DynamicRegistrationBean<ServletRegistration.Dynamic> {

    private static final String[] DEFAULT_MAPPINGS = {"/*"};

    private T servlet;

    private Set<String> urlMappings = new LinkedHashSet<>();

    private boolean alwaysMapUrl = true;

    private int loadOnStartup = -1;

    private MultipartConfigElement multipartConfig;

    public ServletRegistrationBean() {
    }

    public ServletRegistrationBean(T servlet, String... urlMappings) {
        this(servlet, true, urlMappings);
    }

    public ServletRegistrationBean(T servlet, boolean alwaysMapUrl, String... urlMappings) {
        Assert.notNull(servlet, "Servlet must not be null");
        Assert.notNull(urlMappings, "UrlMappings must not be null");
        this.servlet = servlet;
        this.alwaysMapUrl = alwaysMapUrl;
        this.urlMappings.addAll(Arrays.asList(urlMappings));
    }

    public void setServlet(T servlet) {
        Assert.notNull(servlet, "Servlet must not be null");
        this.servlet = servlet;
    }

    public T getServlet() {
        return this.servlet;
    }

    public void setUrlMappings(Collection<String> urlMappings) {
        Assert.notNull(urlMappings, "UrlMappings must not be null");
        this.urlMappings = new LinkedHashSet<>(urlMappings);
    }

    public Collection<String> getUrlMappings() {
        return this.urlMappings;
    }

    public void addUrlMappings(String... urlMappings) {
        Assert.notNull(urlMappings, "UrlMappings must not be null");
        this.urlMappings.addAll(Arrays.asList(urlMappings));
    }

    public void setLoadOnStartup(int loadOnStartup) {
        this.loadOnStartup = loadOnStartup;
    }

    public void setMultipartConfig(MultipartConfigElement multipartConfig) {
        this.multipartConfig = multipartConfig;
    }

    public MultipartConfigElement getMultipartConfig() {
        return this.multipartConfig;
    }

    @Override
    protected String getDescription() {
        Assert.notNull(this.servlet, "Servlet must not be null");
        return "servlet " + getServletName();
    }

    @Override
    protected ServletRegistration.Dynamic addRegistration(String description, ServletContext servletContext) {
        String name = getServletName();
        return servletContext.addServlet(name, this.servlet);
    }

    @Override
    protected void configure(ServletRegistration.Dynamic registration) {
        super.configure(registration);
        String[] urlMapping = StringUtils.toStringArray(this.urlMappings);
        if (urlMapping.length == 0 && this.alwaysMapUrl) {
            // 默认"/*"
            urlMapping = DEFAULT_MAPPINGS;
        }
        if (!ObjectUtils.isEmpty(urlMapping)) {
            registration.addMapping(urlMapping);
        }
        // 默认-1，懒加载，所以初始化（init方法）不是在容器启动过程中，而是在容器收到第一个对该Servlet的请求时
        registration.setLoadOnStartup(this.loadOnStartup);
        if (this.multipartConfig != null) {
            // 文件上传
            registration.setMultipartConfig(this.multipartConfig);
        }
    }

    public String getServletName() {
        return getOrDeduceName(this.servlet);
    }

    @Override
    public String toString() {
        return getServletName() + " urls=" + getUrlMappings();
    }

}
