package org.apache.dubbo.common.store.support;

import org.apache.dubbo.common.store.DataStore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SimpleDataStore implements DataStore {

    private ConcurrentMap<String, ConcurrentMap<String, Object>> data = new ConcurrentHashMap<String, ConcurrentMap<String, Object>>();

    @Override
    public Map<String, Object> get(String componentName) {
        ConcurrentMap<String, Object> value = data.get(componentName);
        if (value == null) {
            return new HashMap<String, Object>();
        }
        return new HashMap<String, Object>(value);
    }

    @Override
    public Object get(String componentName, String key) {
        if (!data.containsKey(componentName)) {
            return null;
        }
        return data.get(componentName).get(key);
    }

    @Override
    public void put(String componentName, String key, Object value) {
        Map<String, Object> componentData = data.get(componentName);
        if (null == componentData) {
            data.putIfAbsent(componentName, new ConcurrentHashMap<String, Object>());
            componentData = data.get(componentName);
        }
        componentData.put(key, value);
    }

    @Override
    public void remove(String componentName, String key) {
        if (!data.containsKey(componentName)) {
            return;
        }
        data.get(componentName).remove(key);
    }

}
