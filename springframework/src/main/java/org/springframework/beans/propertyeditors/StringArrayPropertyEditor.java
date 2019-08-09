package org.springframework.beans.propertyeditors;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.beans.PropertyEditorSupport;

public class StringArrayPropertyEditor extends PropertyEditorSupport {

    public static final String DEFAULT_SEPARATOR = ",";

    private final String separator;

    @Nullable
    private final String charsToDelete;

    private final boolean emptyArrayAsNull;

    private final boolean trimValues;

    public StringArrayPropertyEditor() {
        this(DEFAULT_SEPARATOR, null, false);
    }

    public StringArrayPropertyEditor(String separator) {
        this(separator, null, false);
    }

    public StringArrayPropertyEditor(String separator, boolean emptyArrayAsNull) {
        this(separator, null, emptyArrayAsNull);
    }

    public StringArrayPropertyEditor(String separator, boolean emptyArrayAsNull, boolean trimValues) {
        this(separator, null, emptyArrayAsNull, trimValues);
    }

    public StringArrayPropertyEditor(String separator, @Nullable String charsToDelete, boolean emptyArrayAsNull) {
        this(separator, charsToDelete, emptyArrayAsNull, true);
    }

    public StringArrayPropertyEditor(String separator, @Nullable String charsToDelete, boolean emptyArrayAsNull, boolean trimValues) {
        this.separator = separator;
        this.charsToDelete = charsToDelete;
        this.emptyArrayAsNull = emptyArrayAsNull;
        this.trimValues = trimValues;
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        String[] array = StringUtils.delimitedListToStringArray(text, this.separator, this.charsToDelete);
        if (this.trimValues) {
            array = StringUtils.trimArrayElements(array);
        }
        if (this.emptyArrayAsNull && array.length == 0) {
            setValue(null);
        } else {
            setValue(array);
        }
    }

    @Override
    public String getAsText() {
        return StringUtils.arrayToDelimitedString(ObjectUtils.toObjectArray(getValue()), this.separator);
    }

}
