package org.springframework.beans.factory.support;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.Mergeable;
import org.springframework.lang.Nullable;

import java.util.Properties;

@SuppressWarnings("serial")
public class ManagedProperties extends Properties implements Mergeable, BeanMetadataElement {

    @Nullable
    private Object source;

    private boolean mergeEnabled;

    public void setSource(@Nullable Object source) {
        this.source = source;
    }

    @Override
    @Nullable
    public Object getSource() {
        return this.source;
    }

    public void setMergeEnabled(boolean mergeEnabled) {
        this.mergeEnabled = mergeEnabled;
    }

    @Override
    public boolean isMergeEnabled() {
        return this.mergeEnabled;
    }

    @Override
    public Object merge(@Nullable Object parent) {
        if (!this.mergeEnabled) {
            throw new IllegalStateException("Not allowed to merge when the 'mergeEnabled' property is set to 'false'");
        }
        if (parent == null) {
            return this;
        }
        if (!(parent instanceof Properties)) {
            throw new IllegalArgumentException("Cannot merge with object of type [" + parent.getClass() + "]");
        }
        Properties merged = new ManagedProperties();
        merged.putAll((Properties) parent);
        merged.putAll(this);
        return merged;
    }

}
