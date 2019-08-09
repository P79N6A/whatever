package org.springframework.web.context.request;

import org.springframework.lang.Nullable;

import java.security.Principal;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public interface WebRequest extends RequestAttributes {

    @Nullable
    String getHeader(String headerName);

    @Nullable
    String[] getHeaderValues(String headerName);

    Iterator<String> getHeaderNames();

    @Nullable
    String getParameter(String paramName);

    @Nullable
    String[] getParameterValues(String paramName);

    Iterator<String> getParameterNames();

    Map<String, String[]> getParameterMap();

    Locale getLocale();

    String getContextPath();

    @Nullable
    String getRemoteUser();

    @Nullable
    Principal getUserPrincipal();

    boolean isUserInRole(String role);

    boolean isSecure();

    boolean checkNotModified(long lastModifiedTimestamp);

    boolean checkNotModified(String etag);

    boolean checkNotModified(@Nullable String etag, long lastModifiedTimestamp);

    String getDescription(boolean includeClientInfo);

}
