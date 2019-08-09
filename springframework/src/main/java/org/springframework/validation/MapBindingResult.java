package org.springframework.validation;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.Map;

@SuppressWarnings("serial")
public class MapBindingResult extends AbstractBindingResult implements Serializable {

    private final Map<?, ?> target;

    public MapBindingResult(Map<?, ?> target, String objectName) {
        super(objectName);
        Assert.notNull(target, "Target Map must not be null");
        this.target = target;
    }

    public final Map<?, ?> getTargetMap() {
        return this.target;
    }

    @Override
    public final Object getTarget() {
        return this.target;
    }

    @Override
    @Nullable
    protected Object getActualFieldValue(String field) {
        return this.target.get(field);
    }

}
