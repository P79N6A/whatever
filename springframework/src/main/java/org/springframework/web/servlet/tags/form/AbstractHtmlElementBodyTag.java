package org.springframework.web.servlet.tags.form;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTag;
import java.io.IOException;

@SuppressWarnings("serial")
public abstract class AbstractHtmlElementBodyTag extends AbstractHtmlElementTag implements BodyTag {

    @Nullable
    private BodyContent bodyContent;

    @Nullable
    private TagWriter tagWriter;

    @Override
    protected int writeTagContent(TagWriter tagWriter) throws JspException {
        onWriteTagContent();
        this.tagWriter = tagWriter;
        if (shouldRender()) {
            exposeAttributes();
            return EVAL_BODY_BUFFERED;
        } else {
            return SKIP_BODY;
        }
    }

    @Override
    public int doEndTag() throws JspException {
        if (shouldRender()) {
            Assert.state(this.tagWriter != null, "No TagWriter set");
            if (this.bodyContent != null && StringUtils.hasText(this.bodyContent.getString())) {
                renderFromBodyContent(this.bodyContent, this.tagWriter);
            } else {
                renderDefaultContent(this.tagWriter);
            }
        }
        return EVAL_PAGE;
    }

    protected void renderFromBodyContent(BodyContent bodyContent, TagWriter tagWriter) throws JspException {
        flushBufferedBodyContent(bodyContent);
    }

    @Override
    public void doFinally() {
        super.doFinally();
        removeAttributes();
        this.tagWriter = null;
        this.bodyContent = null;
    }
    //---------------------------------------------------------------------
    // Template methods
    //---------------------------------------------------------------------

    protected void onWriteTagContent() {
    }

    protected boolean shouldRender() throws JspException {
        return true;
    }

    protected void exposeAttributes() throws JspException {
    }

    protected void removeAttributes() {
    }

    protected void flushBufferedBodyContent(BodyContent bodyContent) throws JspException {
        try {
            bodyContent.writeOut(bodyContent.getEnclosingWriter());
        } catch (IOException ex) {
            throw new JspException("Unable to write buffered body content.", ex);
        }
    }

    protected abstract void renderDefaultContent(TagWriter tagWriter) throws JspException;
    //---------------------------------------------------------------------
    // BodyTag implementation
    //---------------------------------------------------------------------

    @Override
    public void doInitBody() throws JspException {
        // no op
    }

    @Override
    public void setBodyContent(BodyContent bodyContent) {
        this.bodyContent = bodyContent;
    }

}
