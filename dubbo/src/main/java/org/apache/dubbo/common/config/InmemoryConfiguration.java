package org.apache.dubbo.common.config;

import java.util.LinkedHashMap;
import java.util.Map;

public class InmemoryConfiguration extends AbstractPrefixConfiguration {

    private Map<String, String> store = new LinkedHashMap<>();

    public InmemoryConfiguration(String prefix, String id) {
        super(prefix, id);
    }

    public InmemoryConfiguration() {
        this(null, null);
    }

    @Override
    public Object getInternalProperty(String key) {
        return store.get(key);
    }

    public void addProperty(String key, String value) {
        store.put(key, value);
    }

    public void addProperties(Map<String, String> properties) {
        if (properties != null) {
            this.store.putAll(properties);
        }
    }

    public void setProperties(Map<String, String> properties) {
        if (properties != null) {
            this.store = properties;
        }
    }

}
