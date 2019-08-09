package org.springframework.web.servlet.config.annotation;

import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

public class ViewControllerRegistration {

    private final String urlPath;

    private final ParameterizableViewController controller = new ParameterizableViewController();

    public ViewControllerRegistration(String urlPath) {
        Assert.notNull(urlPath, "'urlPath' is required.");
        this.urlPath = urlPath;
    }

    public ViewControllerRegistration setStatusCode(HttpStatus statusCode) {
        this.controller.setStatusCode(statusCode);
        return this;
    }

    public void setViewName(String viewName) {
        this.controller.setViewName(viewName);
    }

    protected void setApplicationContext(@Nullable ApplicationContext applicationContext) {
        this.controller.setApplicationContext(applicationContext);
    }

    protected String getUrlPath() {
        return this.urlPath;
    }

    protected ParameterizableViewController getViewController() {
        return this.controller;
    }

}
