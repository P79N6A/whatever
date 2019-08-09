package org.springframework.web.context.request;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import java.lang.reflect.Method;
import java.util.Map;

public class FacesRequestAttributes implements RequestAttributes {

    private static final Log logger = LogFactory.getLog(FacesRequestAttributes.class);

    private final FacesContext facesContext;

    public FacesRequestAttributes(FacesContext facesContext) {
        Assert.notNull(facesContext, "FacesContext must not be null");
        this.facesContext = facesContext;
    }

    protected final FacesContext getFacesContext() {
        return this.facesContext;
    }

    protected final ExternalContext getExternalContext() {
        return getFacesContext().getExternalContext();
    }

    protected Map<String, Object> getAttributeMap(int scope) {
        if (scope == SCOPE_REQUEST) {
            return getExternalContext().getRequestMap();
        } else {
            return getExternalContext().getSessionMap();
        }
    }

    @Override
    public Object getAttribute(String name, int scope) {
        return getAttributeMap(scope).get(name);
    }

    @Override
    public void setAttribute(String name, Object value, int scope) {
        getAttributeMap(scope).put(name, value);
    }

    @Override
    public void removeAttribute(String name, int scope) {
        getAttributeMap(scope).remove(name);
    }

    @Override
    public String[] getAttributeNames(int scope) {
        return StringUtils.toStringArray(getAttributeMap(scope).keySet());
    }

    @Override
    public void registerDestructionCallback(String name, Runnable callback, int scope) {
        if (logger.isWarnEnabled()) {
            logger.warn("Could not register destruction callback [" + callback + "] for attribute '" + name + "' because FacesRequestAttributes does not support such callbacks");
        }
    }

    @Override
    public Object resolveReference(String key) {
        if (REFERENCE_REQUEST.equals(key)) {
            return getExternalContext().getRequest();
        } else if (REFERENCE_SESSION.equals(key)) {
            return getExternalContext().getSession(true);
        } else if ("application".equals(key)) {
            return getExternalContext().getContext();
        } else if ("requestScope".equals(key)) {
            return getExternalContext().getRequestMap();
        } else if ("sessionScope".equals(key)) {
            return getExternalContext().getSessionMap();
        } else if ("applicationScope".equals(key)) {
            return getExternalContext().getApplicationMap();
        } else if ("facesContext".equals(key)) {
            return getFacesContext();
        } else if ("cookie".equals(key)) {
            return getExternalContext().getRequestCookieMap();
        } else if ("header".equals(key)) {
            return getExternalContext().getRequestHeaderMap();
        } else if ("headerValues".equals(key)) {
            return getExternalContext().getRequestHeaderValuesMap();
        } else if ("param".equals(key)) {
            return getExternalContext().getRequestParameterMap();
        } else if ("paramValues".equals(key)) {
            return getExternalContext().getRequestParameterValuesMap();
        } else if ("initParam".equals(key)) {
            return getExternalContext().getInitParameterMap();
        } else if ("view".equals(key)) {
            return getFacesContext().getViewRoot();
        } else if ("viewScope".equals(key)) {
            return getFacesContext().getViewRoot().getViewMap();
        } else if ("flash".equals(key)) {
            return getExternalContext().getFlash();
        } else if ("resource".equals(key)) {
            return getFacesContext().getApplication().getResourceHandler();
        } else {
            return null;
        }
    }

    @Override
    public String getSessionId() {
        Object session = getExternalContext().getSession(true);
        try {
            // HttpSession has a getId() method.
            Method getIdMethod = session.getClass().getMethod("getId");
            return String.valueOf(ReflectionUtils.invokeMethod(getIdMethod, session));
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException("Session object [" + session + "] does not have a getId() method");
        }
    }

    @Override
    public Object getSessionMutex() {
        // Enforce presence of a session first to allow listeners to create the mutex attribute
        ExternalContext externalContext = getExternalContext();
        Object session = externalContext.getSession(true);
        Object mutex = externalContext.getSessionMap().get(WebUtils.SESSION_MUTEX_ATTRIBUTE);
        if (mutex == null) {
            mutex = (session != null ? session : externalContext);
        }
        return mutex;
    }

}
