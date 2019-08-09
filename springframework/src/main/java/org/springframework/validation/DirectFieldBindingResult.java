package org.springframework.validation;

import org.springframework.beans.ConfigurablePropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.lang.Nullable;

@SuppressWarnings("serial")
public class DirectFieldBindingResult extends AbstractPropertyBindingResult {

    @Nullable
    private final Object target;

    private final boolean autoGrowNestedPaths;

    @Nullable
    private transient ConfigurablePropertyAccessor directFieldAccessor;

    public DirectFieldBindingResult(@Nullable Object target, String objectName) {
        this(target, objectName, true);
    }

    public DirectFieldBindingResult(@Nullable Object target, String objectName, boolean autoGrowNestedPaths) {
        super(objectName);
        this.target = target;
        this.autoGrowNestedPaths = autoGrowNestedPaths;
    }

    @Override
    @Nullable
    public final Object getTarget() {
        return this.target;
    }

    @Override
    public final ConfigurablePropertyAccessor getPropertyAccessor() {
        if (this.directFieldAccessor == null) {
            this.directFieldAccessor = createDirectFieldAccessor();
            this.directFieldAccessor.setExtractOldValueForEditor(true);
            this.directFieldAccessor.setAutoGrowNestedPaths(this.autoGrowNestedPaths);
        }
        return this.directFieldAccessor;
    }

    protected ConfigurablePropertyAccessor createDirectFieldAccessor() {
        if (this.target == null) {
            throw new IllegalStateException("Cannot access fields on null target instance '" + getObjectName() + "'");
        }
        return PropertyAccessorFactory.forDirectFieldAccess(this.target);
    }

}
