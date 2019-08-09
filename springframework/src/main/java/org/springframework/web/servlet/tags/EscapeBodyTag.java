package org.springframework.web.servlet.tags;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.util.JavaScriptUtils;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTag;
import java.io.IOException;

@SuppressWarnings("serial")
public class EscapeBodyTag extends HtmlEscapingAwareTag implements BodyTag {

    private boolean javaScriptEscape = false;

    @Nullable
    private BodyContent bodyContent;

    public void setJavaScriptEscape(boolean javaScriptEscape) throws JspException {
        this.javaScriptEscape = javaScriptEscape;
    }

    @Override
    protected int doStartTagInternal() {
        // do nothing
        return EVAL_BODY_BUFFERED;
    }

    @Override
    public void doInitBody() {
        // do nothing
    }

    @Override
    public void setBodyContent(BodyContent bodyContent) {
        this.bodyContent = bodyContent;
    }

    @Override
    public int doAfterBody() throws JspException {
        try {
            String content = readBodyContent();
            // HTML and/or JavaScript escape, if demanded
            content = htmlEscape(content);
            content = (this.javaScriptEscape ? JavaScriptUtils.javaScriptEscape(content) : content);
            writeBodyContent(content);
        } catch (IOException ex) {
            throw new JspException("Could not write escaped body", ex);
        }
        return (SKIP_BODY);
    }

    protected String readBodyContent() throws IOException {
        Assert.state(this.bodyContent != null, "No BodyContent set");
        return this.bodyContent.getString();
    }

    protected void writeBodyContent(String content) throws IOException {
        Assert.state(this.bodyContent != null, "No BodyContent set");
        this.bodyContent.getEnclosingWriter().print(content);
    }

}
