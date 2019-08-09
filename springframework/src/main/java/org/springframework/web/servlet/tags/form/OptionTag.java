package org.springframework.web.servlet.tags.form;

import org.springframework.lang.Nullable;
import org.springframework.web.servlet.support.BindStatus;
import org.springframework.web.util.TagUtils;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTag;

@SuppressWarnings("serial")
public class OptionTag extends AbstractHtmlElementBodyTag implements BodyTag {

    public static final String VALUE_VARIABLE_NAME = "value";

    public static final String DISPLAY_VALUE_VARIABLE_NAME = "displayValue";

    private static final String SELECTED_ATTRIBUTE = "selected";

    private static final String VALUE_ATTRIBUTE = VALUE_VARIABLE_NAME;

    private static final String DISABLED_ATTRIBUTE = "disabled";

    @Nullable
    private Object value;

    @Nullable
    private String label;

    @Nullable
    private Object oldValue;

    @Nullable
    private Object oldDisplayValue;

    private boolean disabled;

    public void setValue(Object value) {
        this.value = value;
    }

    @Nullable
    protected Object getValue() {
        return this.value;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    protected boolean isDisabled() {
        return this.disabled;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Nullable
    protected String getLabel() {
        return this.label;
    }

    @Override
    protected void renderDefaultContent(TagWriter tagWriter) throws JspException {
        Object value = this.pageContext.getAttribute(VALUE_VARIABLE_NAME);
        String label = getLabelValue(value);
        renderOption(value, label, tagWriter);
    }

    @Override
    protected void renderFromBodyContent(BodyContent bodyContent, TagWriter tagWriter) throws JspException {
        Object value = this.pageContext.getAttribute(VALUE_VARIABLE_NAME);
        String label = bodyContent.getString();
        renderOption(value, label, tagWriter);
    }

    @Override
    protected void onWriteTagContent() {
        assertUnderSelectTag();
    }

    @Override
    protected void exposeAttributes() throws JspException {
        Object value = resolveValue();
        this.oldValue = this.pageContext.getAttribute(VALUE_VARIABLE_NAME);
        this.pageContext.setAttribute(VALUE_VARIABLE_NAME, value);
        this.oldDisplayValue = this.pageContext.getAttribute(DISPLAY_VALUE_VARIABLE_NAME);
        this.pageContext.setAttribute(DISPLAY_VALUE_VARIABLE_NAME, getDisplayString(value, getBindStatus().getEditor()));
    }

    @Override
    protected BindStatus getBindStatus() {
        return (BindStatus) this.pageContext.getAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE);
    }

    @Override
    protected void removeAttributes() {
        if (this.oldValue != null) {
            this.pageContext.setAttribute(VALUE_ATTRIBUTE, this.oldValue);
            this.oldValue = null;
        } else {
            this.pageContext.removeAttribute(VALUE_VARIABLE_NAME);
        }
        if (this.oldDisplayValue != null) {
            this.pageContext.setAttribute(DISPLAY_VALUE_VARIABLE_NAME, this.oldDisplayValue);
            this.oldDisplayValue = null;
        } else {
            this.pageContext.removeAttribute(DISPLAY_VALUE_VARIABLE_NAME);
        }
    }

    private void renderOption(Object value, String label, TagWriter tagWriter) throws JspException {
        tagWriter.startTag("option");
        writeOptionalAttribute(tagWriter, "id", resolveId());
        writeOptionalAttributes(tagWriter);
        String renderedValue = getDisplayString(value, getBindStatus().getEditor());
        renderedValue = processFieldValue(getSelectTag().getName(), renderedValue, "option");
        tagWriter.writeAttribute(VALUE_ATTRIBUTE, renderedValue);
        if (isSelected(value)) {
            tagWriter.writeAttribute(SELECTED_ATTRIBUTE, SELECTED_ATTRIBUTE);
        }
        if (isDisabled()) {
            tagWriter.writeAttribute(DISABLED_ATTRIBUTE, "disabled");
        }
        tagWriter.appendValue(label);
        tagWriter.endTag();
    }

    @Override
    protected String autogenerateId() throws JspException {
        return null;
    }

    private String getLabelValue(Object resolvedValue) throws JspException {
        String label = getLabel();
        Object labelObj = (label == null ? resolvedValue : evaluate("label", label));
        return getDisplayString(labelObj, getBindStatus().getEditor());
    }

    private void assertUnderSelectTag() {
        TagUtils.assertHasAncestorOfType(this, SelectTag.class, "option", "select");
    }

    private SelectTag getSelectTag() {
        return (SelectTag) findAncestorWithClass(this, SelectTag.class);
    }

    private boolean isSelected(Object resolvedValue) {
        return SelectedValueComparator.isSelected(getBindStatus(), resolvedValue);
    }

    @Nullable
    private Object resolveValue() throws JspException {
        return evaluate(VALUE_VARIABLE_NAME, getValue());
    }

}
