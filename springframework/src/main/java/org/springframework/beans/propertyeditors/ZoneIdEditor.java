package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.time.ZoneId;

public class ZoneIdEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        setValue(ZoneId.of(text));
    }

    @Override
    public String getAsText() {
        ZoneId value = (ZoneId) getValue();
        return (value != null ? value.getId() : "");
    }

}
