package org.apache.dubbo.cache.support.lru;

import org.apache.dubbo.cache.Cache;
import org.apache.dubbo.cache.support.AbstractCacheFactory;
import org.apache.dubbo.common.URL;

public class LruCacheFactory extends AbstractCacheFactory {

    @Override
    protected Cache createCache(URL url) {
        return new LruCache(url);
    }

}
