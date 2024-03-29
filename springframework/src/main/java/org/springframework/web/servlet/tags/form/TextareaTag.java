package org.springframework.web.servlet.tags.form;

import org.springframework.lang.Nullable;

import javax.servlet.jsp.JspException;

@SuppressWarnings("serial")
public class TextareaTag extends AbstractHtmlInputElementTag {

    public static final String ROWS_ATTRIBUTE = "rows";

    public static final String COLS_ATTRIBUTE = "cols";

    public static final String ONSELECT_ATTRIBUTE = "onselect";

    @Nullable
    private String rows;

    @Nullable
    private String cols;

    @Nullable
    private String onselect;

    public void setRows(String rows) {
        this.rows = rows;
    }

    @Nullable
    protected String getRows() {
        return this.rows;
    }

    public void setCols(String cols) {
        this.cols = cols;
    }

    @Nullable
    protected String getCols() {
        return this.cols;
    }

    public void setOnselect(String onselect) {
        this.onselect = onselect;
    }

    @Nullable
    protected String getOnselect() {
        return this.onselect;
    }

    @Override
    protected int writeTagContent(TagWriter tagWriter) throws JspException {
        tagWriter.startTag("textarea");
        writeDefaultAttributes(tagWriter);
        writeOptionalAttribute(tagWriter, ROWS_ATTRIBUTE, getRows());
        writeOptionalAttribute(tagWriter, COLS_ATTRIBUTE, getCols());
        writeOptionalAttribute(tagWriter, ONSELECT_ATTRIBUTE, getOnselect());
        String value = getDisplayString(getBoundValue(), getPropertyEditor());
        tagWriter.appendValue("\r\n" + processFieldValue(getName(), value, "textarea"));
        tagWriter.endTag();
        return SKIP_BODY;
    }

}
