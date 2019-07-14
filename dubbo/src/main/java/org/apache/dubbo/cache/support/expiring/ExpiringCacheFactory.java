package org.apache.dubbo.cache.support.expiring;

import org.apache.dubbo.cache.Cache;
import org.apache.dubbo.cache.support.AbstractCacheFactory;
import org.apache.dubbo.common.URL;

public class ExpiringCacheFactory extends AbstractCacheFactory {

    @Override
    protected Cache createCache(URL url) {
        return new ExpiringCache(url);
    }

}
