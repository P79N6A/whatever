package org.springframework.web.servlet.config.annotation;

import org.springframework.cache.Cache;
import org.springframework.http.CacheControl;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ResourceHandlerRegistration {

    private final String[] pathPatterns;

    private final List<String> locationValues = new ArrayList<>();

    @Nullable
    private Integer cachePeriod;

    @Nullable
    private CacheControl cacheControl;

    @Nullable
    private ResourceChainRegistration resourceChainRegistration;

    public ResourceHandlerRegistration(String... pathPatterns) {
        Assert.notEmpty(pathPatterns, "At least one path pattern is required for resource handling.");
        this.pathPatterns = pathPatterns;
    }

    public ResourceHandlerRegistration addResourceLocations(String... resourceLocations) {
        this.locationValues.addAll(Arrays.asList(resourceLocations));
        return this;
    }

    public ResourceHandlerRegistration setCachePeriod(Integer cachePeriod) {
        this.cachePeriod = cachePeriod;
        return this;
    }

    public ResourceHandlerRegistration setCacheControl(CacheControl cacheControl) {
        this.cacheControl = cacheControl;
        return this;
    }

    public ResourceChainRegistration resourceChain(boolean cacheResources) {
        this.resourceChainRegistration = new ResourceChainRegistration(cacheResources);
        return this.resourceChainRegistration;
    }

    public ResourceChainRegistration resourceChain(boolean cacheResources, Cache cache) {
        this.resourceChainRegistration = new ResourceChainRegistration(cacheResources, cache);
        return this.resourceChainRegistration;
    }

    protected String[] getPathPatterns() {
        return this.pathPatterns;
    }

    protected ResourceHttpRequestHandler getRequestHandler() {
        ResourceHttpRequestHandler handler = new ResourceHttpRequestHandler();
        if (this.resourceChainRegistration != null) {
            handler.setResourceResolvers(this.resourceChainRegistration.getResourceResolvers());
            handler.setResourceTransformers(this.resourceChainRegistration.getResourceTransformers());
        }
        handler.setLocationValues(this.locationValues);
        if (this.cacheControl != null) {
            handler.setCacheControl(this.cacheControl);
        } else if (this.cachePeriod != null) {
            handler.setCacheSeconds(this.cachePeriod);
        }
        return handler;
    }

}
