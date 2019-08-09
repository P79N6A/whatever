package org.springframework.util;

import org.springframework.lang.Nullable;

import java.util.Comparator;
import java.util.Map;

public interface RouteMatcher {

    Route parseRoute(String routeValue);

    boolean isPattern(String route);

    String combine(String pattern1, String pattern2);

    boolean match(String pattern, Route route);

    @Nullable
    Map<String, String> matchAndExtract(String pattern, Route route);

    Comparator<String> getPatternComparator(Route route);

    interface Route {

        String value();

    }

}
