package org.springframework.web.servlet.tags.form;

import javax.servlet.jsp.JspException;

@SuppressWarnings("serial")
public class RadioButtonTag extends AbstractSingleCheckedElementTag {

    @Override
    protected void writeTagDetails(TagWriter tagWriter) throws JspException {
        tagWriter.writeAttribute("type", getInputType());
        Object resolvedValue = evaluate("value", getValue());
        renderFromValue(resolvedValue, tagWriter);
    }

    @Override
    protected String getInputType() {
        return "radio";
    }

}
