package org.springframework.web.servlet.mvc.condition;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.cors.CorsUtils;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

public final class RequestMethodsRequestCondition extends AbstractRequestCondition<RequestMethodsRequestCondition> {

    private static final Map<String, RequestMethodsRequestCondition> requestMethodConditionCache;

    static {
        requestMethodConditionCache = new HashMap<>(RequestMethod.values().length);
        for (RequestMethod method : RequestMethod.values()) {
            requestMethodConditionCache.put(method.name(), new RequestMethodsRequestCondition(method));
        }
    }

    private final Set<RequestMethod> methods;

    public RequestMethodsRequestCondition(RequestMethod... requestMethods) {
        this(Arrays.asList(requestMethods));
    }

    private RequestMethodsRequestCondition(Collection<RequestMethod> requestMethods) {
        this.methods = Collections.unmodifiableSet(new LinkedHashSet<>(requestMethods));
    }

    public Set<RequestMethod> getMethods() {
        return this.methods;
    }

    @Override
    protected Collection<RequestMethod> getContent() {
        return this.methods;
    }

    @Override
    protected String getToStringInfix() {
        return " || ";
    }

    @Override
    public RequestMethodsRequestCondition combine(RequestMethodsRequestCondition other) {
        Set<RequestMethod> set = new LinkedHashSet<>(this.methods);
        set.addAll(other.methods);
        return new RequestMethodsRequestCondition(set);
    }

    @Override
    @Nullable
    public RequestMethodsRequestCondition getMatchingCondition(HttpServletRequest request) {
        if (CorsUtils.isPreFlightRequest(request)) {
            return matchPreFlight(request);
        }
        if (getMethods().isEmpty()) {
            if (RequestMethod.OPTIONS.name().equals(request.getMethod()) && !DispatcherType.ERROR.equals(request.getDispatcherType())) {
                return null; // We handle OPTIONS transparently, so don't match if no explicit declarations
            }
            return this;
        }
        return matchRequestMethod(request.getMethod());
    }

    @Nullable
    private RequestMethodsRequestCondition matchPreFlight(HttpServletRequest request) {
        if (getMethods().isEmpty()) {
            return this;
        }
        String expectedMethod = request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
        return matchRequestMethod(expectedMethod);
    }

    @Nullable
    private RequestMethodsRequestCondition matchRequestMethod(String httpMethodValue) {
        HttpMethod httpMethod = HttpMethod.resolve(httpMethodValue);
        if (httpMethod != null) {
            for (RequestMethod method : getMethods()) {
                if (httpMethod.matches(method.name())) {
                    return requestMethodConditionCache.get(method.name());
                }
            }
            if (httpMethod == HttpMethod.HEAD && getMethods().contains(RequestMethod.GET)) {
                return requestMethodConditionCache.get(HttpMethod.GET.name());
            }
        }
        return null;
    }

    @Override
    public int compareTo(RequestMethodsRequestCondition other, HttpServletRequest request) {
        if (other.methods.size() != this.methods.size()) {
            return other.methods.size() - this.methods.size();
        } else if (this.methods.size() == 1) {
            if (this.methods.contains(RequestMethod.HEAD) && other.methods.contains(RequestMethod.GET)) {
                return -1;
            } else if (this.methods.contains(RequestMethod.GET) && other.methods.contains(RequestMethod.HEAD)) {
                return 1;
            }
        }
        return 0;
    }

}
