package org.springframework.web.cors;

import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class CorsConfiguration {

    public static final String ALL = "*";

    private static final List<HttpMethod> DEFAULT_METHODS = Collections.unmodifiableList(Arrays.asList(HttpMethod.GET, HttpMethod.HEAD));

    private static final List<String> DEFAULT_PERMIT_METHODS = Collections.unmodifiableList(Arrays.asList(HttpMethod.GET.name(), HttpMethod.HEAD.name(), HttpMethod.POST.name()));

    private static final List<String> DEFAULT_PERMIT_ALL = Collections.unmodifiableList(Collections.singletonList(ALL));

    @Nullable
    private List<String> allowedOrigins;

    @Nullable
    private List<String> allowedMethods;

    @Nullable
    private List<HttpMethod> resolvedMethods = DEFAULT_METHODS;

    @Nullable
    private List<String> allowedHeaders;

    @Nullable
    private List<String> exposedHeaders;

    @Nullable
    private Boolean allowCredentials;

    @Nullable
    private Long maxAge;

    public CorsConfiguration() {
    }

    public CorsConfiguration(CorsConfiguration other) {
        this.allowedOrigins = other.allowedOrigins;
        this.allowedMethods = other.allowedMethods;
        this.resolvedMethods = other.resolvedMethods;
        this.allowedHeaders = other.allowedHeaders;
        this.exposedHeaders = other.exposedHeaders;
        this.allowCredentials = other.allowCredentials;
        this.maxAge = other.maxAge;
    }

    public void setAllowedOrigins(@Nullable List<String> allowedOrigins) {
        this.allowedOrigins = (allowedOrigins != null ? new ArrayList<>(allowedOrigins) : null);
    }

    @Nullable
    public List<String> getAllowedOrigins() {
        return this.allowedOrigins;
    }

    public void addAllowedOrigin(String origin) {
        if (this.allowedOrigins == null) {
            this.allowedOrigins = new ArrayList<>(4);
        } else if (this.allowedOrigins == DEFAULT_PERMIT_ALL) {
            setAllowedOrigins(DEFAULT_PERMIT_ALL);
        }
        this.allowedOrigins.add(origin);
    }

    public void setAllowedMethods(@Nullable List<String> allowedMethods) {
        this.allowedMethods = (allowedMethods != null ? new ArrayList<>(allowedMethods) : null);
        if (!CollectionUtils.isEmpty(allowedMethods)) {
            this.resolvedMethods = new ArrayList<>(allowedMethods.size());
            for (String method : allowedMethods) {
                if (ALL.equals(method)) {
                    this.resolvedMethods = null;
                    break;
                }
                this.resolvedMethods.add(HttpMethod.resolve(method));
            }
        } else {
            this.resolvedMethods = DEFAULT_METHODS;
        }
    }

    @Nullable
    public List<String> getAllowedMethods() {
        return this.allowedMethods;
    }

    public void addAllowedMethod(HttpMethod method) {
        addAllowedMethod(method.name());
    }

    public void addAllowedMethod(String method) {
        if (StringUtils.hasText(method)) {
            if (this.allowedMethods == null) {
                this.allowedMethods = new ArrayList<>(4);
                this.resolvedMethods = new ArrayList<>(4);
            } else if (this.allowedMethods == DEFAULT_PERMIT_METHODS) {
                setAllowedMethods(DEFAULT_PERMIT_METHODS);
            }
            this.allowedMethods.add(method);
            if (ALL.equals(method)) {
                this.resolvedMethods = null;
            } else if (this.resolvedMethods != null) {
                this.resolvedMethods.add(HttpMethod.resolve(method));
            }
        }
    }

    public void setAllowedHeaders(@Nullable List<String> allowedHeaders) {
        this.allowedHeaders = (allowedHeaders != null ? new ArrayList<>(allowedHeaders) : null);
    }

    @Nullable
    public List<String> getAllowedHeaders() {
        return this.allowedHeaders;
    }

    public void addAllowedHeader(String allowedHeader) {
        if (this.allowedHeaders == null) {
            this.allowedHeaders = new ArrayList<>(4);
        } else if (this.allowedHeaders == DEFAULT_PERMIT_ALL) {
            setAllowedHeaders(DEFAULT_PERMIT_ALL);
        }
        this.allowedHeaders.add(allowedHeader);
    }

    public void setExposedHeaders(@Nullable List<String> exposedHeaders) {
        if (exposedHeaders != null && exposedHeaders.contains(ALL)) {
            throw new IllegalArgumentException("'*' is not a valid exposed header value");
        }
        this.exposedHeaders = (exposedHeaders != null ? new ArrayList<>(exposedHeaders) : null);
    }

    @Nullable
    public List<String> getExposedHeaders() {
        return this.exposedHeaders;
    }

    public void addExposedHeader(String exposedHeader) {
        if (ALL.equals(exposedHeader)) {
            throw new IllegalArgumentException("'*' is not a valid exposed header value");
        }
        if (this.exposedHeaders == null) {
            this.exposedHeaders = new ArrayList<>(4);
        }
        this.exposedHeaders.add(exposedHeader);
    }

    public void setAllowCredentials(@Nullable Boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }

    @Nullable
    public Boolean getAllowCredentials() {
        return this.allowCredentials;
    }

    public void setMaxAge(Duration maxAge) {
        this.maxAge = maxAge.getSeconds();
    }

    public void setMaxAge(@Nullable Long maxAge) {
        this.maxAge = maxAge;
    }

    @Nullable
    public Long getMaxAge() {
        return this.maxAge;
    }

    public CorsConfiguration applyPermitDefaultValues() {
        if (this.allowedOrigins == null) {
            this.allowedOrigins = DEFAULT_PERMIT_ALL;
        }
        if (this.allowedMethods == null) {
            this.allowedMethods = DEFAULT_PERMIT_METHODS;
            this.resolvedMethods = DEFAULT_PERMIT_METHODS.stream().map(HttpMethod::resolve).collect(Collectors.toList());
        }
        if (this.allowedHeaders == null) {
            this.allowedHeaders = DEFAULT_PERMIT_ALL;
        }
        if (this.maxAge == null) {
            this.maxAge = 1800L;
        }
        return this;
    }

    @Nullable
    public CorsConfiguration combine(@Nullable CorsConfiguration other) {
        if (other == null) {
            return this;
        }
        CorsConfiguration config = new CorsConfiguration(this);
        config.setAllowedOrigins(combine(getAllowedOrigins(), other.getAllowedOrigins()));
        config.setAllowedMethods(combine(getAllowedMethods(), other.getAllowedMethods()));
        config.setAllowedHeaders(combine(getAllowedHeaders(), other.getAllowedHeaders()));
        config.setExposedHeaders(combine(getExposedHeaders(), other.getExposedHeaders()));
        Boolean allowCredentials = other.getAllowCredentials();
        if (allowCredentials != null) {
            config.setAllowCredentials(allowCredentials);
        }
        Long maxAge = other.getMaxAge();
        if (maxAge != null) {
            config.setMaxAge(maxAge);
        }
        return config;
    }

    private List<String> combine(@Nullable List<String> source, @Nullable List<String> other) {
        if (other == null) {
            return (source != null ? source : Collections.emptyList());
        }
        if (source == null) {
            return other;
        }
        if (source == DEFAULT_PERMIT_ALL || source == DEFAULT_PERMIT_METHODS) {
            return other;
        }
        if (other == DEFAULT_PERMIT_ALL || other == DEFAULT_PERMIT_METHODS) {
            return source;
        }
        if (source.contains(ALL) || other.contains(ALL)) {
            return new ArrayList<>(Collections.singletonList(ALL));
        }
        Set<String> combined = new LinkedHashSet<>(source);
        combined.addAll(other);
        return new ArrayList<>(combined);
    }

    @Nullable
    public String checkOrigin(@Nullable String requestOrigin) {
        if (!StringUtils.hasText(requestOrigin)) {
            return null;
        }
        if (ObjectUtils.isEmpty(this.allowedOrigins)) {
            return null;
        }
        if (this.allowedOrigins.contains(ALL)) {
            if (this.allowCredentials != Boolean.TRUE) {
                return ALL;
            } else {
                return requestOrigin;
            }
        }
        for (String allowedOrigin : this.allowedOrigins) {
            if (requestOrigin.equalsIgnoreCase(allowedOrigin)) {
                return requestOrigin;
            }
        }
        return null;
    }

    @Nullable
    public List<HttpMethod> checkHttpMethod(@Nullable HttpMethod requestMethod) {
        if (requestMethod == null) {
            return null;
        }
        if (this.resolvedMethods == null) {
            return Collections.singletonList(requestMethod);
        }
        return (this.resolvedMethods.contains(requestMethod) ? this.resolvedMethods : null);
    }

    @Nullable
    public List<String> checkHeaders(@Nullable List<String> requestHeaders) {
        if (requestHeaders == null) {
            return null;
        }
        if (requestHeaders.isEmpty()) {
            return Collections.emptyList();
        }
        if (ObjectUtils.isEmpty(this.allowedHeaders)) {
            return null;
        }
        boolean allowAnyHeader = this.allowedHeaders.contains(ALL);
        List<String> result = new ArrayList<>(requestHeaders.size());
        for (String requestHeader : requestHeaders) {
            if (StringUtils.hasText(requestHeader)) {
                requestHeader = requestHeader.trim();
                if (allowAnyHeader) {
                    result.add(requestHeader);
                } else {
                    for (String allowedHeader : this.allowedHeaders) {
                        if (requestHeader.equalsIgnoreCase(allowedHeader)) {
                            result.add(requestHeader);
                            break;
                        }
                    }
                }
            }
        }
        return (result.isEmpty() ? null : result);
    }

}
