package org.springframework.web.servlet.config.annotation;

import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ViewControllerRegistry {

    @Nullable
    private ApplicationContext applicationContext;

    private final List<ViewControllerRegistration> registrations = new ArrayList<>(4);

    private final List<RedirectViewControllerRegistration> redirectRegistrations = new ArrayList<>(10);

    private int order = 1;

    public ViewControllerRegistry(@Nullable ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public ViewControllerRegistration addViewController(String urlPath) {
        ViewControllerRegistration registration = new ViewControllerRegistration(urlPath);
        registration.setApplicationContext(this.applicationContext);
        this.registrations.add(registration);
        return registration;
    }

    public RedirectViewControllerRegistration addRedirectViewController(String urlPath, String redirectUrl) {
        RedirectViewControllerRegistration registration = new RedirectViewControllerRegistration(urlPath, redirectUrl);
        registration.setApplicationContext(this.applicationContext);
        this.redirectRegistrations.add(registration);
        return registration;
    }

    public void addStatusController(String urlPath, HttpStatus statusCode) {
        ViewControllerRegistration registration = new ViewControllerRegistration(urlPath);
        registration.setApplicationContext(this.applicationContext);
        registration.setStatusCode(statusCode);
        registration.getViewController().setStatusOnly(true);
        this.registrations.add(registration);
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Nullable
    protected SimpleUrlHandlerMapping buildHandlerMapping() {
        if (this.registrations.isEmpty() && this.redirectRegistrations.isEmpty()) {
            return null;
        }
        Map<String, Object> urlMap = new LinkedHashMap<>();
        for (ViewControllerRegistration registration : this.registrations) {
            urlMap.put(registration.getUrlPath(), registration.getViewController());
        }
        for (RedirectViewControllerRegistration registration : this.redirectRegistrations) {
            urlMap.put(registration.getUrlPath(), registration.getViewController());
        }
        SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
        handlerMapping.setUrlMap(urlMap);
        handlerMapping.setOrder(this.order);
        return handlerMapping;
    }

}
