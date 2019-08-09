package org.springframework.beans.propertyeditors;

import org.springframework.util.StringUtils;

import java.beans.PropertyEditorSupport;

public class LocaleEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(String text) {
        setValue(StringUtils.parseLocaleString(text));
    }

    @Override
    public String getAsText() {
        Object value = getValue();
        return (value != null ? value.toString() : "");
    }

}
