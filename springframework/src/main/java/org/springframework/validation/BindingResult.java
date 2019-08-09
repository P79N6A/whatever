package org.springframework.validation;

import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.lang.Nullable;

import java.beans.PropertyEditor;
import java.util.Map;

public interface BindingResult extends Errors {

    String MODEL_KEY_PREFIX = BindingResult.class.getName() + ".";

    @Nullable
    Object getTarget();

    Map<String, Object> getModel();

    @Nullable
    Object getRawFieldValue(String field);

    @Nullable
    PropertyEditor findEditor(@Nullable String field, @Nullable Class<?> valueType);

    @Nullable
    PropertyEditorRegistry getPropertyEditorRegistry();

    String[] resolveMessageCodes(String errorCode);

    String[] resolveMessageCodes(String errorCode, String field);

    void addError(ObjectError error);

    default void recordFieldValue(String field, Class<?> type, @Nullable Object value) {
    }

    default void recordSuppressedField(String field) {
    }

    default String[] getSuppressedFields() {
        return new String[0];
    }

}
