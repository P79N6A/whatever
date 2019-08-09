package org.springframework.web.servlet.tags.form;

import javax.servlet.jsp.JspException;

@SuppressWarnings("serial")
public class HiddenInputTag extends AbstractHtmlElementTag {

    public static final String DISABLED_ATTRIBUTE = "disabled";

    private boolean disabled;

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public boolean isDisabled() {
        return this.disabled;
    }

    @Override
    protected boolean isValidDynamicAttribute(String localName, Object value) {
        return !"type".equals(localName);
    }

    @Override
    protected int writeTagContent(TagWriter tagWriter) throws JspException {
        tagWriter.startTag("input");
        writeDefaultAttributes(tagWriter);
        tagWriter.writeAttribute("type", "hidden");
        if (isDisabled()) {
            tagWriter.writeAttribute(DISABLED_ATTRIBUTE, "disabled");
        }
        String value = getDisplayString(getBoundValue(), getPropertyEditor());
        tagWriter.writeAttribute("value", processFieldValue(getName(), value, "hidden"));
        tagWriter.endTag();
        return SKIP_BODY;
    }

}
