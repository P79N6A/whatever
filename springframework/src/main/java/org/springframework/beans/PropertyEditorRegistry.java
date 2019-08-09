package org.springframework.beans;

import org.springframework.lang.Nullable;

import java.beans.PropertyEditor;

public interface PropertyEditorRegistry {

    void registerCustomEditor(Class<?> requiredType, PropertyEditor propertyEditor);

    void registerCustomEditor(@Nullable Class<?> requiredType, @Nullable String propertyPath, PropertyEditor propertyEditor);

    @Nullable
    PropertyEditor findCustomEditor(@Nullable Class<?> requiredType, @Nullable String propertyPath);

}
