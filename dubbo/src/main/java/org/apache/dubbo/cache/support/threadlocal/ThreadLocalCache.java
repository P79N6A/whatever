package org.apache.dubbo.cache.support.threadlocal;

import org.apache.dubbo.cache.Cache;
import org.apache.dubbo.common.URL;

import java.util.HashMap;
import java.util.Map;

public class ThreadLocalCache implements Cache {

    private final ThreadLocal<Map<Object, Object>> store;

    public ThreadLocalCache(URL url) {
        this.store = ThreadLocal.withInitial(HashMap::new);
    }

    @Override
    public void put(Object key, Object value) {
        store.get().put(key, value);
    }

    @Override
    public Object get(Object key) {
        return store.get().get(key);
    }

}
