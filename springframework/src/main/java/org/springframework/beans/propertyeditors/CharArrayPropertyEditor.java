package org.springframework.beans.propertyeditors;

import org.springframework.lang.Nullable;

import java.beans.PropertyEditorSupport;

public class CharArrayPropertyEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(@Nullable String text) {
        setValue(text != null ? text.toCharArray() : null);
    }

    @Override
    public String getAsText() {
        char[] value = (char[]) getValue();
        return (value != null ? new String(value) : "");
    }

}
