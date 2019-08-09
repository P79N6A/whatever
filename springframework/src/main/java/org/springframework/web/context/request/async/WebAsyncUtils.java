package org.springframework.web.context.request.async;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class WebAsyncUtils {

    public static final String WEB_ASYNC_MANAGER_ATTRIBUTE = WebAsyncManager.class.getName() + ".WEB_ASYNC_MANAGER";

    public static WebAsyncManager getAsyncManager(ServletRequest servletRequest) {
        WebAsyncManager asyncManager = null;
        Object asyncManagerAttr = servletRequest.getAttribute(WEB_ASYNC_MANAGER_ATTRIBUTE);
        if (asyncManagerAttr instanceof WebAsyncManager) {
            asyncManager = (WebAsyncManager) asyncManagerAttr;
        }
        if (asyncManager == null) {
            asyncManager = new WebAsyncManager();
            servletRequest.setAttribute(WEB_ASYNC_MANAGER_ATTRIBUTE, asyncManager);
        }
        return asyncManager;
    }

    public static WebAsyncManager getAsyncManager(WebRequest webRequest) {
        int scope = RequestAttributes.SCOPE_REQUEST;
        WebAsyncManager asyncManager = null;
        Object asyncManagerAttr = webRequest.getAttribute(WEB_ASYNC_MANAGER_ATTRIBUTE, scope);
        if (asyncManagerAttr instanceof WebAsyncManager) {
            asyncManager = (WebAsyncManager) asyncManagerAttr;
        }
        if (asyncManager == null) {
            asyncManager = new WebAsyncManager();
            webRequest.setAttribute(WEB_ASYNC_MANAGER_ATTRIBUTE, asyncManager, scope);
        }
        return asyncManager;
    }

    public static AsyncWebRequest createAsyncWebRequest(HttpServletRequest request, HttpServletResponse response) {
        return new StandardServletAsyncWebRequest(request, response);
    }

}
