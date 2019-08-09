package org.springframework.web.servlet.config.annotation;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.MappedInterceptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InterceptorRegistration {

    private final HandlerInterceptor interceptor;

    private final List<String> includePatterns = new ArrayList<>();

    private final List<String> excludePatterns = new ArrayList<>();

    @Nullable
    private PathMatcher pathMatcher;

    private int order = 0;

    public InterceptorRegistration(HandlerInterceptor interceptor) {
        Assert.notNull(interceptor, "Interceptor is required");
        this.interceptor = interceptor;
    }

    public InterceptorRegistration addPathPatterns(String... patterns) {
        return addPathPatterns(Arrays.asList(patterns));
    }

    public InterceptorRegistration addPathPatterns(List<String> patterns) {
        this.includePatterns.addAll(patterns);
        return this;
    }

    public InterceptorRegistration excludePathPatterns(String... patterns) {
        return excludePathPatterns(Arrays.asList(patterns));
    }

    public InterceptorRegistration excludePathPatterns(List<String> patterns) {
        this.excludePatterns.addAll(patterns);
        return this;
    }

    public InterceptorRegistration pathMatcher(PathMatcher pathMatcher) {
        this.pathMatcher = pathMatcher;
        return this;
    }

    public InterceptorRegistration order(int order) {
        this.order = order;
        return this;
    }

    protected int getOrder() {
        return this.order;
    }

    protected Object getInterceptor() {
        if (this.includePatterns.isEmpty() && this.excludePatterns.isEmpty()) {
            return this.interceptor;
        }
        String[] include = StringUtils.toStringArray(this.includePatterns);
        String[] exclude = StringUtils.toStringArray(this.excludePatterns);
        MappedInterceptor mappedInterceptor = new MappedInterceptor(include, exclude, this.interceptor);
        if (this.pathMatcher != null) {
            mappedInterceptor.setPathMatcher(this.pathMatcher);
        }
        return mappedInterceptor;
    }

}
