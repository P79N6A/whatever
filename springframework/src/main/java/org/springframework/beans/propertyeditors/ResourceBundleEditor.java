package org.springframework.beans.propertyeditors;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.beans.PropertyEditorSupport;
import java.util.Locale;
import java.util.ResourceBundle;

public class ResourceBundleEditor extends PropertyEditorSupport {

    public static final String BASE_NAME_SEPARATOR = "_";

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        Assert.hasText(text, "'text' must not be empty");
        String name = text.trim();
        int separator = name.indexOf(BASE_NAME_SEPARATOR);
        if (separator == -1) {
            setValue(ResourceBundle.getBundle(name));
        } else {
            // The name potentially contains locale information
            String baseName = name.substring(0, separator);
            if (!StringUtils.hasText(baseName)) {
                throw new IllegalArgumentException("Invalid ResourceBundle name: '" + text + "'");
            }
            String localeString = name.substring(separator + 1);
            Locale locale = StringUtils.parseLocaleString(localeString);
            setValue(locale != null ? ResourceBundle.getBundle(baseName, locale) : ResourceBundle.getBundle(baseName));
        }
    }

}
