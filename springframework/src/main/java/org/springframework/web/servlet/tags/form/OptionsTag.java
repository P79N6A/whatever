package org.springframework.web.servlet.tags.form;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.BindStatus;
import org.springframework.web.util.TagUtils;

import javax.servlet.jsp.JspException;

@SuppressWarnings("serial")
public class OptionsTag extends AbstractHtmlElementTag {

    @Nullable
    private Object items;

    @Nullable
    private String itemValue;

    @Nullable
    private String itemLabel;

    private boolean disabled;

    public void setItems(Object items) {
        this.items = items;
    }

    @Nullable
    protected Object getItems() {
        return this.items;
    }

    public void setItemValue(String itemValue) {
        Assert.hasText(itemValue, "'itemValue' must not be empty");
        this.itemValue = itemValue;
    }

    @Nullable
    protected String getItemValue() {
        return this.itemValue;
    }

    public void setItemLabel(String itemLabel) {
        Assert.hasText(itemLabel, "'itemLabel' must not be empty");
        this.itemLabel = itemLabel;
    }

    @Nullable
    protected String getItemLabel() {
        return this.itemLabel;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    protected boolean isDisabled() {
        return this.disabled;
    }

    @Override
    protected int writeTagContent(TagWriter tagWriter) throws JspException {
        SelectTag selectTag = getSelectTag();
        Object items = getItems();
        Object itemsObject = null;
        if (items != null) {
            itemsObject = (items instanceof String ? evaluate("items", items) : items);
        } else {
            Class<?> selectTagBoundType = selectTag.getBindStatus().getValueType();
            if (selectTagBoundType != null && selectTagBoundType.isEnum()) {
                itemsObject = selectTagBoundType.getEnumConstants();
            }
        }
        if (itemsObject != null) {
            String selectName = selectTag.getName();
            String itemValue = getItemValue();
            String itemLabel = getItemLabel();
            String valueProperty = (itemValue != null ? ObjectUtils.getDisplayString(evaluate("itemValue", itemValue)) : null);
            String labelProperty = (itemLabel != null ? ObjectUtils.getDisplayString(evaluate("itemLabel", itemLabel)) : null);
            OptionsWriter optionWriter = new OptionsWriter(selectName, itemsObject, valueProperty, labelProperty);
            optionWriter.writeOptions(tagWriter);
        }
        return SKIP_BODY;
    }

    @Override
    protected String resolveId() throws JspException {
        Object id = evaluate("id", getId());
        if (id != null) {
            String idString = id.toString();
            return (StringUtils.hasText(idString) ? TagIdGenerator.nextId(idString, this.pageContext) : null);
        }
        return null;
    }

    private SelectTag getSelectTag() {
        TagUtils.assertHasAncestorOfType(this, SelectTag.class, "options", "select");
        return (SelectTag) findAncestorWithClass(this, SelectTag.class);
    }

    @Override
    protected BindStatus getBindStatus() {
        return (BindStatus) this.pageContext.getAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE);
    }

    private class OptionsWriter extends OptionWriter {

        @Nullable
        private final String selectName;

        public OptionsWriter(@Nullable String selectName, Object optionSource, @Nullable String valueProperty, @Nullable String labelProperty) {
            super(optionSource, getBindStatus(), valueProperty, labelProperty, isHtmlEscape());
            this.selectName = selectName;
        }

        @Override
        protected boolean isOptionDisabled() throws JspException {
            return isDisabled();
        }

        @Override
        protected void writeCommonAttributes(TagWriter tagWriter) throws JspException {
            writeOptionalAttribute(tagWriter, "id", resolveId());
            writeOptionalAttributes(tagWriter);
        }

        @Override
        protected String processOptionValue(String value) {
            return processFieldValue(this.selectName, value, "option");
        }

    }

}
