package org.springframework.web.servlet.tags.form;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import javax.servlet.jsp.JspException;

@SuppressWarnings("serial")
public abstract class AbstractSingleCheckedElementTag extends AbstractCheckedElementTag {

    @Nullable
    private Object value;

    @Nullable
    private Object label;

    public void setValue(Object value) {
        this.value = value;
    }

    @Nullable
    protected Object getValue() {
        return this.value;
    }

    public void setLabel(Object label) {
        this.label = label;
    }

    @Nullable
    protected Object getLabel() {
        return this.label;
    }

    @Override
    protected int writeTagContent(TagWriter tagWriter) throws JspException {
        tagWriter.startTag("input");
        String id = resolveId();
        writeOptionalAttribute(tagWriter, "id", id);
        writeOptionalAttribute(tagWriter, "name", getName());
        writeOptionalAttributes(tagWriter);
        writeTagDetails(tagWriter);
        tagWriter.endTag();
        Object resolvedLabel = evaluate("label", getLabel());
        if (resolvedLabel != null) {
            Assert.state(id != null, "Label id is required");
            tagWriter.startTag("label");
            tagWriter.writeAttribute("for", id);
            tagWriter.appendValue(convertToDisplayString(resolvedLabel));
            tagWriter.endTag();
        }
        return SKIP_BODY;
    }

    protected abstract void writeTagDetails(TagWriter tagWriter) throws JspException;

}
