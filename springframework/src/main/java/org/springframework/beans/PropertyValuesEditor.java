package org.springframework.beans;

import org.springframework.beans.propertyeditors.PropertiesEditor;

import java.beans.PropertyEditorSupport;
import java.util.Properties;

public class PropertyValuesEditor extends PropertyEditorSupport {

    private final PropertiesEditor propertiesEditor = new PropertiesEditor();

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        this.propertiesEditor.setAsText(text);
        Properties props = (Properties) this.propertiesEditor.getValue();
        setValue(new MutablePropertyValues(props));
    }

}

