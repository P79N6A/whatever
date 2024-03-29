package org.springframework.validation;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.ConfigurablePropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.lang.Nullable;

import java.io.Serializable;

@SuppressWarnings("serial")
public class BeanPropertyBindingResult extends AbstractPropertyBindingResult implements Serializable {

    @Nullable
    private final Object target;

    private final boolean autoGrowNestedPaths;

    private final int autoGrowCollectionLimit;

    @Nullable
    private transient BeanWrapper beanWrapper;

    public BeanPropertyBindingResult(@Nullable Object target, String objectName) {
        this(target, objectName, true, Integer.MAX_VALUE);
    }

    public BeanPropertyBindingResult(@Nullable Object target, String objectName, boolean autoGrowNestedPaths, int autoGrowCollectionLimit) {
        super(objectName);
        this.target = target;
        this.autoGrowNestedPaths = autoGrowNestedPaths;
        this.autoGrowCollectionLimit = autoGrowCollectionLimit;
    }

    @Override
    @Nullable
    public final Object getTarget() {
        return this.target;
    }

    @Override
    public final ConfigurablePropertyAccessor getPropertyAccessor() {
        if (this.beanWrapper == null) {
            this.beanWrapper = createBeanWrapper();
            this.beanWrapper.setExtractOldValueForEditor(true);
            this.beanWrapper.setAutoGrowNestedPaths(this.autoGrowNestedPaths);
            this.beanWrapper.setAutoGrowCollectionLimit(this.autoGrowCollectionLimit);
        }
        return this.beanWrapper;
    }

    protected BeanWrapper createBeanWrapper() {
        if (this.target == null) {
            throw new IllegalStateException("Cannot access properties on null bean instance '" + getObjectName() + "'");
        }
        return PropertyAccessorFactory.forBeanPropertyAccess(this.target);
    }

}
