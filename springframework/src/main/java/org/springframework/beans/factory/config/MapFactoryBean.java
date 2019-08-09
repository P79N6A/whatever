package org.springframework.beans.factory.config;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.TypeConverter;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public class MapFactoryBean extends AbstractFactoryBean<Map<Object, Object>> {

    @Nullable
    private Map<?, ?> sourceMap;

    @SuppressWarnings("rawtypes")
    @Nullable
    private Class<? extends Map> targetMapClass;

    public void setSourceMap(Map<?, ?> sourceMap) {
        this.sourceMap = sourceMap;
    }

    @SuppressWarnings("rawtypes")
    public void setTargetMapClass(@Nullable Class<? extends Map> targetMapClass) {
        if (targetMapClass == null) {
            throw new IllegalArgumentException("'targetMapClass' must not be null");
        }
        if (!Map.class.isAssignableFrom(targetMapClass)) {
            throw new IllegalArgumentException("'targetMapClass' must implement [java.util.Map]");
        }
        this.targetMapClass = targetMapClass;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Class<Map> getObjectType() {
        return Map.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Map<Object, Object> createInstance() {
        if (this.sourceMap == null) {
            throw new IllegalArgumentException("'sourceMap' is required");
        }
        Map<Object, Object> result = null;
        if (this.targetMapClass != null) {
            result = BeanUtils.instantiateClass(this.targetMapClass);
        } else {
            result = new LinkedHashMap<>(this.sourceMap.size());
        }
        Class<?> keyType = null;
        Class<?> valueType = null;
        if (this.targetMapClass != null) {
            ResolvableType mapType = ResolvableType.forClass(this.targetMapClass).asMap();
            keyType = mapType.resolveGeneric(0);
            valueType = mapType.resolveGeneric(1);
        }
        if (keyType != null || valueType != null) {
            TypeConverter converter = getBeanTypeConverter();
            for (Map.Entry<?, ?> entry : this.sourceMap.entrySet()) {
                Object convertedKey = converter.convertIfNecessary(entry.getKey(), keyType);
                Object convertedValue = converter.convertIfNecessary(entry.getValue(), valueType);
                result.put(convertedKey, convertedValue);
            }
        } else {
            result.putAll(this.sourceMap);
        }
        return result;
    }

}
