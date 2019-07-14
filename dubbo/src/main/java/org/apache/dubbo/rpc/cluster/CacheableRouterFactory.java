package org.apache.dubbo.rpc.cluster;

import org.apache.dubbo.common.URL;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class CacheableRouterFactory implements RouterFactory {
    private ConcurrentMap<String, Router> routerMap = new ConcurrentHashMap<>();

    @Override
    public Router getRouter(URL url) {
        routerMap.computeIfAbsent(url.getServiceKey(), k -> createRouter(url));
        return routerMap.get(url.getServiceKey());
    }

    protected abstract Router createRouter(URL url);

}
