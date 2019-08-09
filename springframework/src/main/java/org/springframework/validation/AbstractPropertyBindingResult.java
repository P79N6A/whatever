package org.springframework.validation;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.ConfigurablePropertyAccessor;
import org.springframework.beans.PropertyAccessorUtils;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.ConvertingPropertyEditorAdapter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.beans.PropertyEditor;

@SuppressWarnings("serial")
public abstract class AbstractPropertyBindingResult extends AbstractBindingResult {

    @Nullable
    private transient ConversionService conversionService;

    protected AbstractPropertyBindingResult(String objectName) {
        super(objectName);
    }

    public void initConversion(ConversionService conversionService) {
        Assert.notNull(conversionService, "ConversionService must not be null");
        this.conversionService = conversionService;
        if (getTarget() != null) {
            getPropertyAccessor().setConversionService(conversionService);
        }
    }

    @Override
    public PropertyEditorRegistry getPropertyEditorRegistry() {
        return (getTarget() != null ? getPropertyAccessor() : null);
    }

    @Override
    protected String canonicalFieldName(String field) {
        return PropertyAccessorUtils.canonicalPropertyName(field);
    }

    @Override
    @Nullable
    public Class<?> getFieldType(@Nullable String field) {
        return (getTarget() != null ? getPropertyAccessor().getPropertyType(fixedField(field)) : super.getFieldType(field));
    }

    @Override
    @Nullable
    protected Object getActualFieldValue(String field) {
        return getPropertyAccessor().getPropertyValue(field);
    }

    @Override
    protected Object formatFieldValue(String field, @Nullable Object value) {
        String fixedField = fixedField(field);
        // Try custom editor...
        PropertyEditor customEditor = getCustomEditor(fixedField);
        if (customEditor != null) {
            customEditor.setValue(value);
            String textValue = customEditor.getAsText();
            // If the PropertyEditor returned null, there is no appropriate
            // text representation for this value: only use it if non-null.
            if (textValue != null) {
                return textValue;
            }
        }
        if (this.conversionService != null) {
            // Try custom converter...
            TypeDescriptor fieldDesc = getPropertyAccessor().getPropertyTypeDescriptor(fixedField);
            TypeDescriptor strDesc = TypeDescriptor.valueOf(String.class);
            if (fieldDesc != null && this.conversionService.canConvert(fieldDesc, strDesc)) {
                return this.conversionService.convert(value, fieldDesc, strDesc);
            }
        }
        return value;
    }

    @Nullable
    protected PropertyEditor getCustomEditor(String fixedField) {
        Class<?> targetType = getPropertyAccessor().getPropertyType(fixedField);
        PropertyEditor editor = getPropertyAccessor().findCustomEditor(targetType, fixedField);
        if (editor == null) {
            editor = BeanUtils.findEditorByConvention(targetType);
        }
        return editor;
    }

    @Override
    @Nullable
    public PropertyEditor findEditor(@Nullable String field, @Nullable Class<?> valueType) {
        Class<?> valueTypeForLookup = valueType;
        if (valueTypeForLookup == null) {
            valueTypeForLookup = getFieldType(field);
        }
        PropertyEditor editor = super.findEditor(field, valueTypeForLookup);
        if (editor == null && this.conversionService != null) {
            TypeDescriptor td = null;
            if (field != null && getTarget() != null) {
                TypeDescriptor ptd = getPropertyAccessor().getPropertyTypeDescriptor(fixedField(field));
                if (ptd != null && (valueType == null || valueType.isAssignableFrom(ptd.getType()))) {
                    td = ptd;
                }
            }
            if (td == null) {
                td = TypeDescriptor.valueOf(valueTypeForLookup);
            }
            if (this.conversionService.canConvert(TypeDescriptor.valueOf(String.class), td)) {
                editor = new ConvertingPropertyEditorAdapter(this.conversionService, td);
            }
        }
        return editor;
    }

    public abstract ConfigurablePropertyAccessor getPropertyAccessor();

}
