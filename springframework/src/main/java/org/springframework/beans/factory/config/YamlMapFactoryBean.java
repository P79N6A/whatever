package org.springframework.beans.factory.config;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public class YamlMapFactoryBean extends YamlProcessor implements FactoryBean<Map<String, Object>>, InitializingBean {

    private boolean singleton = true;

    @Nullable
    private Map<String, Object> map;

    public void setSingleton(boolean singleton) {
        this.singleton = singleton;
    }

    @Override
    public boolean isSingleton() {
        return this.singleton;
    }

    @Override
    public void afterPropertiesSet() {
        if (isSingleton()) {
            this.map = createMap();
        }
    }

    @Override
    @Nullable
    public Map<String, Object> getObject() {
        return (this.map != null ? this.map : createMap());
    }

    @Override
    public Class<?> getObjectType() {
        return Map.class;
    }

    protected Map<String, Object> createMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        process((properties, map) -> merge(result, map));
        return result;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void merge(Map<String, Object> output, Map<String, Object> map) {
        map.forEach((key, value) -> {
            Object existing = output.get(key);
            if (value instanceof Map && existing instanceof Map) {
                // Inner cast required by Eclipse IDE.
                Map<String, Object> result = new LinkedHashMap<>((Map<String, Object>) existing);
                merge(result, (Map) value);
                output.put(key, result);
            } else {
                output.put(key, value);
            }
        });
    }

}
