package org.apache.dubbo.cache.support;

import org.apache.dubbo.cache.Cache;
import org.apache.dubbo.cache.CacheFactory;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.dubbo.common.constants.CommonConstants.METHOD_KEY;

public abstract class AbstractCacheFactory implements CacheFactory {

    private final ConcurrentMap<String, Cache> caches = new ConcurrentHashMap<String, Cache>();

    @Override
    public Cache getCache(URL url, Invocation invocation) {
        url = url.addParameter(METHOD_KEY, invocation.getMethodName());
        String key = url.toFullString();
        Cache cache = caches.get(key);
        if (cache == null) {
            caches.put(key, createCache(url));
            cache = caches.get(key);
        }
        return cache;
    }

    protected abstract Cache createCache(URL url);

}
