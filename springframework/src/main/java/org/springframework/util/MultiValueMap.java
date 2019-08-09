package org.springframework.util;

import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Map;

public interface MultiValueMap<K, V> extends Map<K, List<V>> {

    @Nullable
    V getFirst(K key);

    void add(K key, @Nullable V value);

    void addAll(K key, List<? extends V> values);

    void addAll(MultiValueMap<K, V> values);

    void set(K key, @Nullable V value);

    void setAll(Map<K, V> values);

    Map<K, V> toSingleValueMap();

}
