package org.springframework.web.servlet.tags.form;

import org.springframework.lang.Nullable;
import org.springframework.web.servlet.tags.HtmlEscapingAwareTag;

import javax.servlet.jsp.JspException;
import java.beans.PropertyEditor;

@SuppressWarnings("serial")
public abstract class AbstractFormTag extends HtmlEscapingAwareTag {

    @Nullable
    protected Object evaluate(String attributeName, @Nullable Object value) throws JspException {
        return value;
    }

    protected final void writeOptionalAttribute(TagWriter tagWriter, String attributeName, @Nullable String value) throws JspException {
        if (value != null) {
            tagWriter.writeOptionalAttributeValue(attributeName, getDisplayString(evaluate(attributeName, value)));
        }
    }

    protected TagWriter createTagWriter() {
        return new TagWriter(this.pageContext);
    }

    @Override
    protected final int doStartTagInternal() throws Exception {
        return writeTagContent(createTagWriter());
    }

    protected String getDisplayString(@Nullable Object value) {
        return ValueFormatter.getDisplayString(value, isHtmlEscape());
    }

    protected String getDisplayString(@Nullable Object value, @Nullable PropertyEditor propertyEditor) {
        return ValueFormatter.getDisplayString(value, propertyEditor, isHtmlEscape());
    }

    @Override
    protected boolean isDefaultHtmlEscape() {
        Boolean defaultHtmlEscape = getRequestContext().getDefaultHtmlEscape();
        return (defaultHtmlEscape == null || defaultHtmlEscape.booleanValue());
    }

    protected abstract int writeTagContent(TagWriter tagWriter) throws JspException;

}
