package org.apache.dubbo.cache;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.rpc.Invocation;

@SPI("lru")
public interface CacheFactory {

    @Adaptive("cache")
    Cache getCache(URL url, Invocation invocation);

}
