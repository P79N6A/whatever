package org.springframework.cache;

import org.springframework.lang.Nullable;

import java.util.Collection;

public interface CacheManager {

    @Nullable
    Cache getCache(String name);

    Collection<String> getCacheNames();

}
