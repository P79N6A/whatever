package org.springframework.beans.propertyeditors;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.beans.PropertyEditorSupport;

public class StringTrimmerEditor extends PropertyEditorSupport {

    @Nullable
    private final String charsToDelete;

    private final boolean emptyAsNull;

    public StringTrimmerEditor(boolean emptyAsNull) {
        this.charsToDelete = null;
        this.emptyAsNull = emptyAsNull;
    }

    public StringTrimmerEditor(String charsToDelete, boolean emptyAsNull) {
        this.charsToDelete = charsToDelete;
        this.emptyAsNull = emptyAsNull;
    }

    @Override
    public void setAsText(@Nullable String text) {
        if (text == null) {
            setValue(null);
        } else {
            String value = text.trim();
            if (this.charsToDelete != null) {
                value = StringUtils.deleteAny(value, this.charsToDelete);
            }
            if (this.emptyAsNull && value.isEmpty()) {
                setValue(null);
            } else {
                setValue(value);
            }
        }
    }

    @Override
    public String getAsText() {
        Object value = getValue();
        return (value != null ? value.toString() : "");
    }

}
