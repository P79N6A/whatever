package org.springframework.beans.propertyeditors;

import org.springframework.lang.Nullable;
import org.springframework.util.NumberUtils;
import org.springframework.util.StringUtils;

import java.beans.PropertyEditorSupport;
import java.text.NumberFormat;

public class CustomNumberEditor extends PropertyEditorSupport {

    private final Class<? extends Number> numberClass;

    @Nullable
    private final NumberFormat numberFormat;

    private final boolean allowEmpty;

    public CustomNumberEditor(Class<? extends Number> numberClass, boolean allowEmpty) throws IllegalArgumentException {
        this(numberClass, null, allowEmpty);
    }

    public CustomNumberEditor(Class<? extends Number> numberClass, @Nullable NumberFormat numberFormat, boolean allowEmpty) throws IllegalArgumentException {
        if (!Number.class.isAssignableFrom(numberClass)) {
            throw new IllegalArgumentException("Property class must be a subclass of Number");
        }
        this.numberClass = numberClass;
        this.numberFormat = numberFormat;
        this.allowEmpty = allowEmpty;
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        if (this.allowEmpty && !StringUtils.hasText(text)) {
            // Treat empty String as null value.
            setValue(null);
        } else if (this.numberFormat != null) {
            // Use given NumberFormat for parsing text.
            setValue(NumberUtils.parseNumber(text, this.numberClass, this.numberFormat));
        } else {
            // Use default valueOf methods for parsing text.
            setValue(NumberUtils.parseNumber(text, this.numberClass));
        }
    }

    @Override
    public void setValue(@Nullable Object value) {
        if (value instanceof Number) {
            super.setValue(NumberUtils.convertNumberToTargetClass((Number) value, this.numberClass));
        } else {
            super.setValue(value);
        }
    }

    @Override
    public String getAsText() {
        Object value = getValue();
        if (value == null) {
            return "";
        }
        if (this.numberFormat != null) {
            // Use NumberFormat for rendering value.
            return this.numberFormat.format(value);
        } else {
            // Use toString method for rendering value.
            return value.toString();
        }
    }

}
