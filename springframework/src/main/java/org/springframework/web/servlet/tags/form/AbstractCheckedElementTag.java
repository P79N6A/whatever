package org.springframework.web.servlet.tags.form;

import org.springframework.lang.Nullable;

import javax.servlet.jsp.JspException;

@SuppressWarnings("serial")
public abstract class AbstractCheckedElementTag extends AbstractHtmlInputElementTag {

    protected void renderFromValue(@Nullable Object value, TagWriter tagWriter) throws JspException {
        renderFromValue(value, value, tagWriter);
    }

    protected void renderFromValue(@Nullable Object item, @Nullable Object value, TagWriter tagWriter) throws JspException {
        String displayValue = convertToDisplayString(value);
        tagWriter.writeAttribute("value", processFieldValue(getName(), displayValue, getInputType()));
        if (isOptionSelected(value) || (value != item && isOptionSelected(item))) {
            tagWriter.writeAttribute("checked", "checked");
        }
    }

    private boolean isOptionSelected(@Nullable Object value) throws JspException {
        return SelectedValueComparator.isSelected(getBindStatus(), value);
    }

    protected void renderFromBoolean(Boolean boundValue, TagWriter tagWriter) throws JspException {
        tagWriter.writeAttribute("value", processFieldValue(getName(), "true", getInputType()));
        if (boundValue) {
            tagWriter.writeAttribute("checked", "checked");
        }
    }

    @Override
    @Nullable
    protected String autogenerateId() throws JspException {
        String id = super.autogenerateId();
        return (id != null ? TagIdGenerator.nextId(id, this.pageContext) : null);
    }

    @Override
    protected abstract int writeTagContent(TagWriter tagWriter) throws JspException;

    @Override
    protected boolean isValidDynamicAttribute(String localName, Object value) {
        return !"type".equals(localName);
    }

    protected abstract String getInputType();

}
