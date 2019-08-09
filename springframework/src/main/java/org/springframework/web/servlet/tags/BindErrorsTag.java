package org.springframework.web.servlet.tags;

import org.springframework.lang.Nullable;
import org.springframework.validation.Errors;

import javax.servlet.ServletException;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

@SuppressWarnings("serial")
public class BindErrorsTag extends HtmlEscapingAwareTag {

    public static final String ERRORS_VARIABLE_NAME = "errors";

    private String name = "";

    @Nullable
    private Errors errors;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    @Override
    protected final int doStartTagInternal() throws ServletException, JspException {
        this.errors = getRequestContext().getErrors(this.name, isHtmlEscape());
        if (this.errors != null && this.errors.hasErrors()) {
            this.pageContext.setAttribute(ERRORS_VARIABLE_NAME, this.errors, PageContext.REQUEST_SCOPE);
            return EVAL_BODY_INCLUDE;
        } else {
            return SKIP_BODY;
        }
    }

    @Override
    public int doEndTag() {
        this.pageContext.removeAttribute(ERRORS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
        return EVAL_PAGE;
    }

    @Nullable
    public final Errors getErrors() {
        return this.errors;
    }

    @Override
    public void doFinally() {
        super.doFinally();
        this.errors = null;
    }

}
