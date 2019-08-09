package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.annotation.AbstractCookieValueMethodArgumentResolver;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

public class ServletCookieValueMethodArgumentResolver extends AbstractCookieValueMethodArgumentResolver {

    private UrlPathHelper urlPathHelper = new UrlPathHelper();

    public ServletCookieValueMethodArgumentResolver(@Nullable ConfigurableBeanFactory beanFactory) {
        super(beanFactory);
    }

    public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
        this.urlPathHelper = urlPathHelper;
    }

    @Override
    @Nullable
    protected Object resolveName(String cookieName, MethodParameter parameter, NativeWebRequest webRequest) throws Exception {
        HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
        Assert.state(servletRequest != null, "No HttpServletRequest");
        Cookie cookieValue = WebUtils.getCookie(servletRequest, cookieName);
        if (Cookie.class.isAssignableFrom(parameter.getNestedParameterType())) {
            return cookieValue;
        } else if (cookieValue != null) {
            return this.urlPathHelper.decodeRequestString(servletRequest, cookieValue.getValue());
        } else {
            return null;
        }
    }

}
