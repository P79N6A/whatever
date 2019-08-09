package org.springframework.web.servlet.config.annotation;

import org.springframework.lang.Nullable;
import org.springframework.util.PathMatcher;
import org.springframework.web.util.UrlPathHelper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

public class PathMatchConfigurer {

    @Nullable
    private Boolean suffixPatternMatch;

    @Nullable
    private Boolean trailingSlashMatch;

    @Nullable
    private Boolean registeredSuffixPatternMatch;

    @Nullable
    private UrlPathHelper urlPathHelper;

    @Nullable
    private PathMatcher pathMatcher;

    @Nullable
    private Map<String, Predicate<Class<?>>> pathPrefixes;

    public PathMatchConfigurer setUseSuffixPatternMatch(Boolean suffixPatternMatch) {
        this.suffixPatternMatch = suffixPatternMatch;
        return this;
    }

    public PathMatchConfigurer setUseTrailingSlashMatch(Boolean trailingSlashMatch) {
        this.trailingSlashMatch = trailingSlashMatch;
        return this;
    }

    public PathMatchConfigurer setUseRegisteredSuffixPatternMatch(Boolean registeredSuffixPatternMatch) {
        this.registeredSuffixPatternMatch = registeredSuffixPatternMatch;
        return this;
    }

    public PathMatchConfigurer setUrlPathHelper(UrlPathHelper urlPathHelper) {
        this.urlPathHelper = urlPathHelper;
        return this;
    }

    public PathMatchConfigurer setPathMatcher(PathMatcher pathMatcher) {
        this.pathMatcher = pathMatcher;
        return this;
    }

    public PathMatchConfigurer addPathPrefix(String prefix, Predicate<Class<?>> predicate) {
        if (this.pathPrefixes == null) {
            this.pathPrefixes = new LinkedHashMap<>();
        }
        this.pathPrefixes.put(prefix, predicate);
        return this;
    }

    @Nullable
    public Boolean isUseSuffixPatternMatch() {
        return this.suffixPatternMatch;
    }

    @Nullable
    public Boolean isUseTrailingSlashMatch() {
        return this.trailingSlashMatch;
    }

    @Nullable
    public Boolean isUseRegisteredSuffixPatternMatch() {
        return this.registeredSuffixPatternMatch;
    }

    @Nullable
    public UrlPathHelper getUrlPathHelper() {
        return this.urlPathHelper;
    }

    @Nullable
    public PathMatcher getPathMatcher() {
        return this.pathMatcher;
    }

    @Nullable
    protected Map<String, Predicate<Class<?>>> getPathPrefixes() {
        return this.pathPrefixes;
    }

}
