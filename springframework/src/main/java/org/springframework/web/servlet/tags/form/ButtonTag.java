package org.springframework.web.servlet.tags.form;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import javax.servlet.jsp.JspException;

@SuppressWarnings("serial")
public class ButtonTag extends AbstractHtmlElementTag {

    public static final String DISABLED_ATTRIBUTE = "disabled";

    @Nullable
    private TagWriter tagWriter;

    @Nullable
    private String name;

    @Nullable
    private String value;

    private boolean disabled;

    public void setName(String name) {
        this.name = name;
    }

    @Override
    @Nullable
    public String getName() {
        return this.name;
    }

    public void setValue(@Nullable String value) {
        this.value = value;
    }

    @Nullable
    public String getValue() {
        return this.value;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public boolean isDisabled() {
        return this.disabled;
    }

    @Override
    protected int writeTagContent(TagWriter tagWriter) throws JspException {
        tagWriter.startTag("button");
        writeDefaultAttributes(tagWriter);
        tagWriter.writeAttribute("type", getType());
        writeValue(tagWriter);
        if (isDisabled()) {
            tagWriter.writeAttribute(DISABLED_ATTRIBUTE, "disabled");
        }
        tagWriter.forceBlock();
        this.tagWriter = tagWriter;
        return EVAL_BODY_INCLUDE;
    }

    protected void writeValue(TagWriter tagWriter) throws JspException {
        String valueToUse = (getValue() != null ? getValue() : getDefaultValue());
        tagWriter.writeAttribute("value", processFieldValue(getName(), valueToUse, getType()));
    }

    protected String getDefaultValue() {
        return "Submit";
    }

    protected String getType() {
        return "submit";
    }

    @Override
    public int doEndTag() throws JspException {
        Assert.state(this.tagWriter != null, "No TagWriter set");
        this.tagWriter.endTag();
        return EVAL_PAGE;
    }

}
