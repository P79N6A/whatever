package org.springframework.util;

import org.springframework.lang.Nullable;

import java.util.Comparator;
import java.util.Map;

public class SimpleRouteMatcher implements RouteMatcher {

    private final PathMatcher pathMatcher;

    public SimpleRouteMatcher(PathMatcher pathMatcher) {
        Assert.notNull(pathMatcher, "PathMatcher is required");
        this.pathMatcher = pathMatcher;
    }

    public PathMatcher getPathMatcher() {
        return this.pathMatcher;
    }

    @Override
    public Route parseRoute(String route) {
        return new DefaultRoute(route);
    }

    @Override
    public boolean isPattern(String route) {
        return this.pathMatcher.isPattern(route);
    }

    @Override
    public String combine(String pattern1, String pattern2) {
        return this.pathMatcher.combine(pattern1, pattern2);
    }

    @Override
    public boolean match(String pattern, Route route) {
        return this.pathMatcher.match(pattern, route.value());
    }

    @Override
    @Nullable
    public Map<String, String> matchAndExtract(String pattern, Route route) {
        if (!match(pattern, route)) {
            return null;
        }
        return this.pathMatcher.extractUriTemplateVariables(pattern, route.value());
    }

    @Override
    public Comparator<String> getPatternComparator(Route route) {
        return this.pathMatcher.getPatternComparator(route.value());
    }

    private static class DefaultRoute implements Route {

        private final String path;

        DefaultRoute(String path) {
            this.path = path;
        }

        @Override
        public String value() {
            return this.path;
        }

    }

}
