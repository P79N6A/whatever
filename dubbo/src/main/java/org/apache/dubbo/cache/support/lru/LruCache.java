package org.apache.dubbo.cache.support.lru;

import org.apache.dubbo.cache.Cache;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.LRUCache;

import java.util.Map;

public class LruCache implements Cache {

    private final Map<Object, Object> store;

    public LruCache(URL url) {
        final int max = url.getParameter("cache.size", 1000);
        this.store = new LRUCache<>(max);
    }

    @Override
    public void put(Object key, Object value) {
        store.put(key, value);
    }

    @Override
    public Object get(Object key) {
        return store.get(key);
    }

}
