package org.springframework.web.context.request;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import java.security.Principal;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public class FacesWebRequest extends FacesRequestAttributes implements NativeWebRequest {

    public FacesWebRequest(FacesContext facesContext) {
        super(facesContext);
    }

    @Override
    public Object getNativeRequest() {
        return getExternalContext().getRequest();
    }

    @Override
    public Object getNativeResponse() {
        return getExternalContext().getResponse();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getNativeRequest(@Nullable Class<T> requiredType) {
        if (requiredType != null) {
            Object request = getExternalContext().getRequest();
            if (requiredType.isInstance(request)) {
                return (T) request;
            }
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getNativeResponse(@Nullable Class<T> requiredType) {
        if (requiredType != null) {
            Object response = getExternalContext().getResponse();
            if (requiredType.isInstance(response)) {
                return (T) response;
            }
        }
        return null;
    }

    @Override
    @Nullable
    public String getHeader(String headerName) {
        return getExternalContext().getRequestHeaderMap().get(headerName);
    }

    @Override
    @Nullable
    public String[] getHeaderValues(String headerName) {
        return getExternalContext().getRequestHeaderValuesMap().get(headerName);
    }

    @Override
    public Iterator<String> getHeaderNames() {
        return getExternalContext().getRequestHeaderMap().keySet().iterator();
    }

    @Override
    @Nullable
    public String getParameter(String paramName) {
        return getExternalContext().getRequestParameterMap().get(paramName);
    }

    @Override
    public Iterator<String> getParameterNames() {
        return getExternalContext().getRequestParameterNames();
    }

    @Override
    @Nullable
    public String[] getParameterValues(String paramName) {
        return getExternalContext().getRequestParameterValuesMap().get(paramName);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return getExternalContext().getRequestParameterValuesMap();
    }

    @Override
    public Locale getLocale() {
        return getFacesContext().getExternalContext().getRequestLocale();
    }

    @Override
    public String getContextPath() {
        return getFacesContext().getExternalContext().getRequestContextPath();
    }

    @Override
    @Nullable
    public String getRemoteUser() {
        return getFacesContext().getExternalContext().getRemoteUser();
    }

    @Override
    @Nullable
    public Principal getUserPrincipal() {
        return getFacesContext().getExternalContext().getUserPrincipal();
    }

    @Override
    public boolean isUserInRole(String role) {
        return getFacesContext().getExternalContext().isUserInRole(role);
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public boolean checkNotModified(long lastModifiedTimestamp) {
        return false;
    }

    @Override
    public boolean checkNotModified(@Nullable String eTag) {
        return false;
    }

    @Override
    public boolean checkNotModified(@Nullable String etag, long lastModifiedTimestamp) {
        return false;
    }

    @Override
    public String getDescription(boolean includeClientInfo) {
        ExternalContext externalContext = getExternalContext();
        StringBuilder sb = new StringBuilder();
        sb.append("context=").append(externalContext.getRequestContextPath());
        if (includeClientInfo) {
            Object session = externalContext.getSession(false);
            if (session != null) {
                sb.append(";session=").append(getSessionId());
            }
            String user = externalContext.getRemoteUser();
            if (StringUtils.hasLength(user)) {
                sb.append(";user=").append(user);
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "FacesWebRequest: " + getDescription(true);
    }

}
