package org.springframework.web.servlet.tags.form;

import org.springframework.web.bind.WebDataBinder;

import javax.servlet.jsp.JspException;

@SuppressWarnings("serial")
public class CheckboxesTag extends AbstractMultiCheckedElementTag {

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
    protected String getInputType() {
        return "checkbox";
    }

}
