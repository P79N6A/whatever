package org.springframework.web.servlet.tags.form;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.servlet.support.BindStatus;

import javax.servlet.jsp.JspException;
import java.util.Collection;
import java.util.Map;

@SuppressWarnings("serial")
public class SelectTag extends AbstractHtmlInputElementTag {

    public static final String LIST_VALUE_PAGE_ATTRIBUTE = "org.springframework.web.servlet.tags.form.SelectTag.listValue";

    private static final Object EMPTY = new Object();

    @Nullable
    private Object items;

    @Nullable
    private String itemValue;

    @Nullable
    private String itemLabel;

    @Nullable
    private String size;

    @Nullable
    private Object multiple;

    @Nullable
    private TagWriter tagWriter;

    public void setItems(@Nullable Object items) {
        this.items = (items != null ? items : EMPTY);
    }

    @Nullable
    protected Object getItems() {
        return this.items;
    }

    public void setItemValue(String itemValue) {
        this.itemValue = itemValue;
    }

    @Nullable
    protected String getItemValue() {
        return this.itemValue;
    }

    public void setItemLabel(String itemLabel) {
        this.itemLabel = itemLabel;
    }

    @Nullable
    protected String getItemLabel() {
        return this.itemLabel;
    }

    public void setSize(String size) {
        this.size = size;
    }

    @Nullable
    protected String getSize() {
        return this.size;
    }

    public void setMultiple(Object multiple) {
        this.multiple = multiple;
    }

    @Nullable
    protected Object getMultiple() {
        return this.multiple;
    }

    @Override
    protected int writeTagContent(TagWriter tagWriter) throws JspException {
        tagWriter.startTag("select");
        writeDefaultAttributes(tagWriter);
        if (isMultiple()) {
            tagWriter.writeAttribute("multiple", "multiple");
        }
        tagWriter.writeOptionalAttributeValue("size", getDisplayString(evaluate("size", getSize())));
        Object items = getItems();
        if (items != null) {
            // Items specified, but might still be empty...
            if (items != EMPTY) {
                Object itemsObject = evaluate("items", items);
                if (itemsObject != null) {
                    final String selectName = getName();
                    String valueProperty = (getItemValue() != null ? ObjectUtils.getDisplayString(evaluate("itemValue", getItemValue())) : null);
                    String labelProperty = (getItemLabel() != null ? ObjectUtils.getDisplayString(evaluate("itemLabel", getItemLabel())) : null);
                    OptionWriter optionWriter = new OptionWriter(itemsObject, getBindStatus(), valueProperty, labelProperty, isHtmlEscape()) {
                        @Override
                        protected String processOptionValue(String resolvedValue) {
                            return processFieldValue(selectName, resolvedValue, "option");
                        }
                    };
                    optionWriter.writeOptions(tagWriter);
                }
            }
            tagWriter.endTag(true);
            writeHiddenTagIfNecessary(tagWriter);
            return SKIP_BODY;
        } else {
            // Using nested <form:option/> tags, so just expose the value in the PageContext...
            tagWriter.forceBlock();
            this.tagWriter = tagWriter;
            this.pageContext.setAttribute(LIST_VALUE_PAGE_ATTRIBUTE, getBindStatus());
            return EVAL_BODY_INCLUDE;
        }
    }

    private void writeHiddenTagIfNecessary(TagWriter tagWriter) throws JspException {
        if (isMultiple()) {
            tagWriter.startTag("input");
            tagWriter.writeAttribute("type", "hidden");
            String name = WebDataBinder.DEFAULT_FIELD_MARKER_PREFIX + getName();
            tagWriter.writeAttribute("name", name);
            tagWriter.writeAttribute("value", processFieldValue(name, "1", "hidden"));
            tagWriter.endTag();
        }
    }

    private boolean isMultiple() throws JspException {
        Object multiple = getMultiple();
        if (multiple != null) {
            String stringValue = multiple.toString();
            return ("multiple".equalsIgnoreCase(stringValue) || Boolean.parseBoolean(stringValue));
        }
        return forceMultiple();
    }

    private boolean forceMultiple() throws JspException {
        BindStatus bindStatus = getBindStatus();
        Class<?> valueType = bindStatus.getValueType();
        if (valueType != null && typeRequiresMultiple(valueType)) {
            return true;
        } else if (bindStatus.getEditor() != null) {
            Object editorValue = bindStatus.getEditor().getValue();
            if (editorValue != null && typeRequiresMultiple(editorValue.getClass())) {
                return true;
            }
        }
        return false;
    }

    private static boolean typeRequiresMultiple(Class<?> type) {
        return (type.isArray() || Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type));
    }

    @Override
    public int doEndTag() throws JspException {
        if (this.tagWriter != null) {
            this.tagWriter.endTag();
            writeHiddenTagIfNecessary(this.tagWriter);
        }
        return EVAL_PAGE;
    }

    @Override
    public void doFinally() {
        super.doFinally();
        this.tagWriter = null;
        this.pageContext.removeAttribute(LIST_VALUE_PAGE_ATTRIBUTE);
    }

}
