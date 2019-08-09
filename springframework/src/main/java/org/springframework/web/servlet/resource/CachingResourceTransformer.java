package org.springframework.web.servlet.resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class CachingResourceTransformer implements ResourceTransformer {

    private static final Log logger = LogFactory.getLog(CachingResourceTransformer.class);

    private final Cache cache;

    public CachingResourceTransformer(Cache cache) {
        Assert.notNull(cache, "Cache is required");
        this.cache = cache;
    }

    public CachingResourceTransformer(CacheManager cacheManager, String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            throw new IllegalArgumentException("Cache '" + cacheName + "' not found");
        }
        this.cache = cache;
    }

    public Cache getCache() {
        return this.cache;
    }

    @Override
    public Resource transform(HttpServletRequest request, Resource resource, ResourceTransformerChain transformerChain) throws IOException {
        Resource transformed = this.cache.get(resource, Resource.class);
        if (transformed != null) {
            logger.trace("Resource resolved from cache");
            return transformed;
        }
        transformed = transformerChain.transform(request, resource);
        this.cache.put(resource, transformed);
        return transformed;
    }

}
