package org.springframework.web.servlet.tags.form;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.servlet.jsp.JspException;

@SuppressWarnings("serial")
public class LabelTag extends AbstractHtmlElementTag {

    private static final String LABEL_TAG = "label";

    private static final String FOR_ATTRIBUTE = "for";

    @Nullable
    private TagWriter tagWriter;

    @Nullable
    private String forId;

    public void setFor(String forId) {
        this.forId = forId;
    }

    @Nullable
    protected String getFor() {
        return this.forId;
    }

    @Override
    protected int writeTagContent(TagWriter tagWriter) throws JspException {
        tagWriter.startTag(LABEL_TAG);
        tagWriter.writeAttribute(FOR_ATTRIBUTE, resolveFor());
        writeDefaultAttributes(tagWriter);
        tagWriter.forceBlock();
        this.tagWriter = tagWriter;
        return EVAL_BODY_INCLUDE;
    }

    @Override
    @Nullable
    protected String getName() throws JspException {
        // This also suppresses the 'id' attribute (which is okay for a <label/>)
        return null;
    }

    protected String resolveFor() throws JspException {
        if (StringUtils.hasText(this.forId)) {
            return getDisplayString(evaluate(FOR_ATTRIBUTE, this.forId));
        } else {
            return autogenerateFor();
        }
    }

    protected String autogenerateFor() throws JspException {
        return StringUtils.deleteAny(getPropertyPath(), "[]");
    }

    @Override
    public int doEndTag() throws JspException {
        Assert.state(this.tagWriter != null, "No TagWriter set");
        this.tagWriter.endTag();
        return EVAL_PAGE;
    }

    @Override
    public void doFinally() {
        super.doFinally();
        this.tagWriter = null;
    }

}
