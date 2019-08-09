package org.springframework.web.servlet.resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

public class ResourceUrlProvider implements ApplicationListener<ContextRefreshedEvent> {

    protected final Log logger = LogFactory.getLog(getClass());

    private UrlPathHelper urlPathHelper = new UrlPathHelper();

    private PathMatcher pathMatcher = new AntPathMatcher();

    private final Map<String, ResourceHttpRequestHandler> handlerMap = new LinkedHashMap<>();

    private boolean autodetect = true;

    public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
        this.urlPathHelper = urlPathHelper;
    }

    public UrlPathHelper getUrlPathHelper() {
        return this.urlPathHelper;
    }

    public void setPathMatcher(PathMatcher pathMatcher) {
        this.pathMatcher = pathMatcher;
    }

    public PathMatcher getPathMatcher() {
        return this.pathMatcher;
    }

    public void setHandlerMap(@Nullable Map<String, ResourceHttpRequestHandler> handlerMap) {
        if (handlerMap != null) {
            this.handlerMap.clear();
            this.handlerMap.putAll(handlerMap);
            this.autodetect = false;
        }
    }

    public Map<String, ResourceHttpRequestHandler> getHandlerMap() {
        return this.handlerMap;
    }

    public boolean isAutodetect() {
        return this.autodetect;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (isAutodetect()) {
            this.handlerMap.clear();
            detectResourceHandlers(event.getApplicationContext());
            if (!this.handlerMap.isEmpty()) {
                this.autodetect = false;
            }
        }
    }

    protected void detectResourceHandlers(ApplicationContext appContext) {
        Map<String, SimpleUrlHandlerMapping> beans = appContext.getBeansOfType(SimpleUrlHandlerMapping.class);
        List<SimpleUrlHandlerMapping> mappings = new ArrayList<>(beans.values());
        AnnotationAwareOrderComparator.sort(mappings);
        for (SimpleUrlHandlerMapping mapping : mappings) {
            for (String pattern : mapping.getHandlerMap().keySet()) {
                Object handler = mapping.getHandlerMap().get(pattern);
                if (handler instanceof ResourceHttpRequestHandler) {
                    ResourceHttpRequestHandler resourceHandler = (ResourceHttpRequestHandler) handler;
                    this.handlerMap.put(pattern, resourceHandler);
                }
            }
        }
        if (this.handlerMap.isEmpty()) {
            logger.trace("No resource handling mappings found");
        }
    }

    @Nullable
    public final String getForRequestUrl(HttpServletRequest request, String requestUrl) {
        int prefixIndex = getLookupPathIndex(request);
        int suffixIndex = getEndPathIndex(requestUrl);
        if (prefixIndex >= suffixIndex) {
            return null;
        }
        String prefix = requestUrl.substring(0, prefixIndex);
        String suffix = requestUrl.substring(suffixIndex);
        String lookupPath = requestUrl.substring(prefixIndex, suffixIndex);
        String resolvedLookupPath = getForLookupPath(lookupPath);
        return (resolvedLookupPath != null ? prefix + resolvedLookupPath + suffix : null);
    }

    private int getLookupPathIndex(HttpServletRequest request) {
        UrlPathHelper pathHelper = getUrlPathHelper();
        String requestUri = pathHelper.getRequestUri(request);
        String lookupPath = this.urlPathHelper.getLookupPathForRequest(request, HandlerMapping.LOOKUP_PATH);
        return requestUri.indexOf(lookupPath);
    }

    private int getEndPathIndex(String lookupPath) {
        int suffixIndex = lookupPath.length();
        int queryIndex = lookupPath.indexOf('?');
        if (queryIndex > 0) {
            suffixIndex = queryIndex;
        }
        int hashIndex = lookupPath.indexOf('#');
        if (hashIndex > 0) {
            suffixIndex = Math.min(suffixIndex, hashIndex);
        }
        return suffixIndex;
    }

    @Nullable
    public final String getForLookupPath(String lookupPath) {
        // Clean duplicate slashes or pathWithinPattern won't match lookupPath
        String previous;
        do {
            previous = lookupPath;
            lookupPath = StringUtils.replace(lookupPath, "//", "/");
        } while (!lookupPath.equals(previous));
        List<String> matchingPatterns = new ArrayList<>();
        for (String pattern : this.handlerMap.keySet()) {
            if (getPathMatcher().match(pattern, lookupPath)) {
                matchingPatterns.add(pattern);
            }
        }
        if (!matchingPatterns.isEmpty()) {
            Comparator<String> patternComparator = getPathMatcher().getPatternComparator(lookupPath);
            matchingPatterns.sort(patternComparator);
            for (String pattern : matchingPatterns) {
                String pathWithinMapping = getPathMatcher().extractPathWithinPattern(pattern, lookupPath);
                String pathMapping = lookupPath.substring(0, lookupPath.indexOf(pathWithinMapping));
                ResourceHttpRequestHandler handler = this.handlerMap.get(pattern);
                ResourceResolverChain chain = new DefaultResourceResolverChain(handler.getResourceResolvers());
                String resolved = chain.resolveUrlPath(pathWithinMapping, handler.getLocations());
                if (resolved == null) {
                    continue;
                }
                return pathMapping + resolved;
            }
        }
        if (logger.isTraceEnabled()) {
            logger.trace("No match for \"" + lookupPath + "\"");
        }
        return null;
    }

}
