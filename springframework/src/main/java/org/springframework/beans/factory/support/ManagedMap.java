package org.springframework.beans.factory.support;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.Mergeable;
import org.springframework.lang.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("serial")
public class ManagedMap<K, V> extends LinkedHashMap<K, V> implements Mergeable, BeanMetadataElement {

    @Nullable
    private Object source;

    @Nullable
    private String keyTypeName;

    @Nullable
    private String valueTypeName;

    private boolean mergeEnabled;

    public ManagedMap() {
    }

    public ManagedMap(int initialCapacity) {
        super(initialCapacity);
    }

    public void setSource(@Nullable Object source) {
        this.source = source;
    }

    @Override
    @Nullable
    public Object getSource() {
        return this.source;
    }

    public void setKeyTypeName(@Nullable String keyTypeName) {
        this.keyTypeName = keyTypeName;
    }

    @Nullable
    public String getKeyTypeName() {
        return this.keyTypeName;
    }

    public void setValueTypeName(@Nullable String valueTypeName) {
        this.valueTypeName = valueTypeName;
    }

    @Nullable
    public String getValueTypeName() {
        return this.valueTypeName;
    }

    public void setMergeEnabled(boolean mergeEnabled) {
        this.mergeEnabled = mergeEnabled;
    }

    @Override
    public boolean isMergeEnabled() {
        return this.mergeEnabled;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object merge(@Nullable Object parent) {
        if (!this.mergeEnabled) {
            throw new IllegalStateException("Not allowed to merge when the 'mergeEnabled' property is set to 'false'");
        }
        if (parent == null) {
            return this;
        }
        if (!(parent instanceof Map)) {
            throw new IllegalArgumentException("Cannot merge with object of type [" + parent.getClass() + "]");
        }
        Map<K, V> merged = new ManagedMap<>();
        merged.putAll((Map<K, V>) parent);
        merged.putAll(this);
        return merged;
    }

}
