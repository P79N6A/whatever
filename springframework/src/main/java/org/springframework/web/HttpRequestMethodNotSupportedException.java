package org.springframework.web;

import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import javax.servlet.ServletException;
import java.util.*;

@SuppressWarnings("serial")
public class HttpRequestMethodNotSupportedException extends ServletException {

    private final String method;

    @Nullable
    private final String[] supportedMethods;

    public HttpRequestMethodNotSupportedException(String method) {
        this(method, (String[]) null);
    }

    public HttpRequestMethodNotSupportedException(String method, String msg) {
        this(method, null, msg);
    }

    public HttpRequestMethodNotSupportedException(String method, @Nullable Collection<String> supportedMethods) {
        this(method, (supportedMethods != null ? StringUtils.toStringArray(supportedMethods) : null));
    }

    public HttpRequestMethodNotSupportedException(String method, @Nullable String[] supportedMethods) {
        this(method, supportedMethods, "Request method '" + method + "' not supported");
    }

    public HttpRequestMethodNotSupportedException(String method, @Nullable String[] supportedMethods, String msg) {
        super(msg);
        this.method = method;
        this.supportedMethods = supportedMethods;
    }

    public String getMethod() {
        return this.method;
    }

    @Nullable
    public String[] getSupportedMethods() {
        return this.supportedMethods;
    }

    @Nullable
    public Set<HttpMethod> getSupportedHttpMethods() {
        if (this.supportedMethods == null) {
            return null;
        }
        List<HttpMethod> supportedMethods = new LinkedList<>();
        for (String value : this.supportedMethods) {
            HttpMethod resolved = HttpMethod.resolve(value);
            if (resolved != null) {
                supportedMethods.add(resolved);
            }
        }
        return EnumSet.copyOf(supportedMethods);
    }

}
