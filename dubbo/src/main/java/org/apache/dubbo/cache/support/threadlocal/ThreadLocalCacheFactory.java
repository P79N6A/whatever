package org.apache.dubbo.cache.support.threadlocal;

import org.apache.dubbo.cache.Cache;
import org.apache.dubbo.cache.support.AbstractCacheFactory;
import org.apache.dubbo.common.URL;

public class ThreadLocalCacheFactory extends AbstractCacheFactory {

    @Override
    protected Cache createCache(URL url) {
        return new ThreadLocalCache(url);
    }

}
