package org.springframework.beans.factory.support;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

@SuppressWarnings("serial")
public class ManagedArray extends ManagedList<Object> {

    @Nullable
    volatile Class<?> resolvedElementType;

    public ManagedArray(String elementTypeName, int size) {
        super(size);
        Assert.notNull(elementTypeName, "elementTypeName must not be null");
        setElementTypeName(elementTypeName);
    }

}
