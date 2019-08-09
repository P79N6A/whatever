package org.springframework.beans;

import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class AbstractPropertyAccessor extends TypeConverterSupport implements ConfigurablePropertyAccessor {

    private boolean extractOldValueForEditor = false;

    private boolean autoGrowNestedPaths = false;

    @Override
    public void setExtractOldValueForEditor(boolean extractOldValueForEditor) {
        this.extractOldValueForEditor = extractOldValueForEditor;
    }

    @Override
    public boolean isExtractOldValueForEditor() {
        return this.extractOldValueForEditor;
    }

    @Override
    public void setAutoGrowNestedPaths(boolean autoGrowNestedPaths) {
        this.autoGrowNestedPaths = autoGrowNestedPaths;
    }

    @Override
    public boolean isAutoGrowNestedPaths() {
        return this.autoGrowNestedPaths;
    }

    @Override
    public void setPropertyValue(PropertyValue pv) throws BeansException {
        setPropertyValue(pv.getName(), pv.getValue());
    }

    @Override
    public void setPropertyValues(Map<?, ?> map) throws BeansException {
        setPropertyValues(new MutablePropertyValues(map));
    }

    @Override
    public void setPropertyValues(PropertyValues pvs) throws BeansException {
        setPropertyValues(pvs, false, false);
    }

    @Override
    public void setPropertyValues(PropertyValues pvs, boolean ignoreUnknown) throws BeansException {
        setPropertyValues(pvs, ignoreUnknown, false);
    }

    @Override
    public void setPropertyValues(PropertyValues pvs, boolean ignoreUnknown, boolean ignoreInvalid) throws BeansException {
        List<PropertyAccessException> propertyAccessExceptions = null;
        List<PropertyValue> propertyValues = (pvs instanceof MutablePropertyValues ? ((MutablePropertyValues) pvs).getPropertyValueList() : Arrays.asList(pvs.getPropertyValues()));
        for (PropertyValue pv : propertyValues) {
            try {
                // This method may throw any BeansException, which won't be caught
                // here, if there is a critical failure such as no matching field.
                // We can attempt to deal only with less serious exceptions.
                // 属性注入
                setPropertyValue(pv);
            } catch (NotWritablePropertyException ex) {
                if (!ignoreUnknown) {
                    throw ex;
                }
                // Otherwise, just ignore it and continue...
            } catch (NullValueInNestedPathException ex) {
                if (!ignoreInvalid) {
                    throw ex;
                }
                // Otherwise, just ignore it and continue...
            } catch (PropertyAccessException ex) {
                if (propertyAccessExceptions == null) {
                    propertyAccessExceptions = new ArrayList<>();
                }
                propertyAccessExceptions.add(ex);
            }
        }
        // If we encountered individual exceptions, throw the composite exception.
        if (propertyAccessExceptions != null) {
            PropertyAccessException[] paeArray = propertyAccessExceptions.toArray(new PropertyAccessException[0]);
            throw new PropertyBatchUpdateException(paeArray);
        }
    }

    // Redefined with public visibility.
    @Override
    @Nullable
    public Class<?> getPropertyType(String propertyPath) {
        return null;
    }

    @Override
    @Nullable
    public abstract Object getPropertyValue(String propertyName) throws BeansException;

    @Override
    public abstract void setPropertyValue(String propertyName, @Nullable Object value) throws BeansException;

}
