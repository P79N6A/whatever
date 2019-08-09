package org.springframework.web.servlet.tags;

import org.springframework.beans.PropertyAccessor;
import org.springframework.lang.Nullable;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.tagext.TryCatchFinally;

@SuppressWarnings("serial")
public class NestedPathTag extends TagSupport implements TryCatchFinally {

    public static final String NESTED_PATH_VARIABLE_NAME = "nestedPath";

    @Nullable
    private String path;

    @Nullable
    private String previousNestedPath;

    public void setPath(@Nullable String path) {
        if (path == null) {
            path = "";
        }
        if (path.length() > 0 && !path.endsWith(PropertyAccessor.NESTED_PROPERTY_SEPARATOR)) {
            path += PropertyAccessor.NESTED_PROPERTY_SEPARATOR;
        }
        this.path = path;
    }

    @Nullable
    public String getPath() {
        return this.path;
    }

    @Override
    public int doStartTag() throws JspException {
        // Save previous nestedPath value, build and expose current nestedPath value.
        // Use request scope to expose nestedPath to included pages too.
        this.previousNestedPath = (String) this.pageContext.getAttribute(NESTED_PATH_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
        String nestedPath = (this.previousNestedPath != null ? this.previousNestedPath + getPath() : getPath());
        this.pageContext.setAttribute(NESTED_PATH_VARIABLE_NAME, nestedPath, PageContext.REQUEST_SCOPE);
        return EVAL_BODY_INCLUDE;
    }

    @Override
    public int doEndTag() {
        if (this.previousNestedPath != null) {
            // Expose previous nestedPath value.
            this.pageContext.setAttribute(NESTED_PATH_VARIABLE_NAME, this.previousNestedPath, PageContext.REQUEST_SCOPE);
        } else {
            // Remove exposed nestedPath value.
            this.pageContext.removeAttribute(NESTED_PATH_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
        }
        return EVAL_PAGE;
    }

    @Override
    public void doCatch(Throwable throwable) throws Throwable {
        throw throwable;
    }

    @Override
    public void doFinally() {
        this.previousNestedPath = null;
    }

}
