package org.apache.dubbo.cache;

public interface Cache {

    void put(Object key, Object value);

    Object get(Object key);

}
