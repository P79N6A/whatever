package org.springframework.web.util.pattern;

import org.springframework.http.server.PathContainer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.RouteMatcher;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PathPatternRouteMatcher implements RouteMatcher {

    private final PathPatternParser parser;

    private final Map<String, PathPattern> pathPatternCache = new ConcurrentHashMap<>();

    public PathPatternRouteMatcher(PathPatternParser parser) {
        Assert.notNull(parser, "PathPatternParser must not be null");
        this.parser = parser;
    }

    @Override
    public Route parseRoute(String routeValue) {
        return new PathContainerRoute(PathContainer.parsePath(routeValue));
    }

    @Override
    public boolean isPattern(String route) {
        return getPathPattern(route).hasPatternSyntax();
    }

    @Override
    public String combine(String pattern1, String pattern2) {
        return getPathPattern(pattern1).combine(getPathPattern(pattern2)).getPatternString();
    }

    @Override
    public boolean match(String pattern, Route route) {
        return getPathPattern(pattern).matches(getPathContainer(route));
    }

    @Override
    @Nullable
    public Map<String, String> matchAndExtract(String pattern, Route route) {
        PathPattern.PathMatchInfo info = getPathPattern(pattern).matchAndExtract(getPathContainer(route));
        return info != null ? info.getUriVariables() : null;
    }

    @Override
    public Comparator<String> getPatternComparator(Route route) {
        return Comparator.comparing(this::getPathPattern);
    }

    private PathPattern getPathPattern(String pattern) {
        return this.pathPatternCache.computeIfAbsent(pattern, this.parser::parse);
    }

    private PathContainer getPathContainer(Route route) {
        Assert.isInstanceOf(PathContainerRoute.class, route);
        return ((PathContainerRoute) route).pathContainer;
    }

    private static class PathContainerRoute implements Route {

        private final PathContainer pathContainer;

        PathContainerRoute(PathContainer pathContainer) {
            this.pathContainer = pathContainer;
        }

        @Override
        public String value() {
            return this.pathContainer.value();
        }

    }

}
