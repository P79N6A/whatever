package org.springframework.web.servlet.tags;

import javax.servlet.jsp.JspException;

@SuppressWarnings("serial")
public class HtmlEscapeTag extends RequestContextAwareTag {

    private boolean defaultHtmlEscape;

    public void setDefaultHtmlEscape(boolean defaultHtmlEscape) {
        this.defaultHtmlEscape = defaultHtmlEscape;
    }

    @Override
    protected int doStartTagInternal() throws JspException {
        getRequestContext().setDefaultHtmlEscape(this.defaultHtmlEscape);
        return EVAL_BODY_INCLUDE;
    }

}
