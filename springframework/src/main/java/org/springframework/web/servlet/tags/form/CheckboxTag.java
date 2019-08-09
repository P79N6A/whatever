package org.springframework.web.servlet.tags.form;

import org.springframework.web.bind.WebDataBinder;

import javax.servlet.jsp.JspException;

@SuppressWarnings("serial")
public class CheckboxTag extends AbstractSingleCheckedElementTag {

    @Override
    protected int writeTagContent(TagWriter tagWriter) throws JspException {
        super.writeTagContent(tagWriter);
        if (!isDisabled()) {
            // Write out the 'field was present' marker.
            tagWriter.startTag("input");
            tagWriter.writeAttribute("type", "hidden");
            String name = WebDataBinder.DEFAULT_FIELD_MARKER_PREFIX + getName();
            tagWriter.writeAttribute("name", name);
            tagWriter.writeAttribute("value", processFieldValue(name, "on", "hidden"));
            tagWriter.endTag();
        }
        return SKIP_BODY;
    }

    @Override
    protected void writeTagDetails(TagWriter tagWriter) throws JspException {
        tagWriter.writeAttribute("type", getInputType());
        Object boundValue = getBoundValue();
        Class<?> valueType = getBindStatus().getValueType();
        if (Boolean.class == valueType || boolean.class == valueType) {
            // the concrete type may not be a Boolean - can be String
            if (boundValue instanceof String) {
                boundValue = Boolean.valueOf((String) boundValue);
            }
            Boolean booleanValue = (boundValue != null ? (Boolean) boundValue : Boolean.FALSE);
            renderFromBoolean(booleanValue, tagWriter);
        } else {
            Object value = getValue();
            if (value == null) {
                throw new IllegalArgumentException("Attribute 'value' is required when binding to non-boolean values");
            }
            Object resolvedValue = (value instanceof String ? evaluate("value", value) : value);
            renderFromValue(resolvedValue, tagWriter);
        }
    }

    @Override
    protected String getInputType() {
        return "checkbox";
    }

}
