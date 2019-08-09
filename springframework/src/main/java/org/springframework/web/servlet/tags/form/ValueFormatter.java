package org.springframework.web.servlet.tags.form;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.web.util.HtmlUtils;

import java.beans.PropertyEditor;

abstract class ValueFormatter {

    public static String getDisplayString(@Nullable Object value, boolean htmlEscape) {
        String displayValue = ObjectUtils.getDisplayString(value);
        return (htmlEscape ? HtmlUtils.htmlEscape(displayValue) : displayValue);
    }

    public static String getDisplayString(@Nullable Object value, @Nullable PropertyEditor propertyEditor, boolean htmlEscape) {
        if (propertyEditor != null && !(value instanceof String)) {
            try {
                propertyEditor.setValue(value);
                String text = propertyEditor.getAsText();
                if (text != null) {
                    return getDisplayString(text, htmlEscape);
                }
            } catch (Throwable ex) {
                // The PropertyEditor might not support this value... pass through.
            }
        }
        return getDisplayString(value, htmlEscape);
    }

}
