package org.springframework.beans;

import org.springframework.core.convert.ConversionService;
import org.springframework.lang.Nullable;

public interface ConfigurablePropertyAccessor extends PropertyAccessor, PropertyEditorRegistry, TypeConverter {

    void setConversionService(@Nullable ConversionService conversionService);

    @Nullable
    ConversionService getConversionService();

    void setExtractOldValueForEditor(boolean extractOldValueForEditor);

    boolean isExtractOldValueForEditor();

    void setAutoGrowNestedPaths(boolean autoGrowNestedPaths);

    boolean isAutoGrowNestedPaths();

}
