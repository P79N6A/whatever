package org.apache.dubbo.cache.support.expiring;

import org.apache.dubbo.cache.Cache;
import org.apache.dubbo.common.URL;

import java.util.Map;

public class ExpiringCache implements Cache {
    private final Map<Object, Object> store;

    public ExpiringCache(URL url) {
        final int secondsToLive = url.getParameter("cache.seconds", 180);
        final int intervalSeconds = url.getParameter("cache.interval", 4);
        ExpiringMap<Object, Object> expiringMap = new ExpiringMap<>(secondsToLive, intervalSeconds);
        expiringMap.getExpireThread().startExpiryIfNotStarted();
        this.store = expiringMap;
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
