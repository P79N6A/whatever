package org.springframework.web.servlet.tags;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.support.BindStatus;

import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;
import java.beans.PropertyEditor;

@SuppressWarnings("serial")
public class BindTag extends HtmlEscapingAwareTag implements EditorAwareTag {

    public static final String STATUS_VARIABLE_NAME = "status";

    private String path = "";

    private boolean ignoreNestedPath = false;

    @Nullable
    private BindStatus status;

    @Nullable
    private Object previousPageStatus;

    @Nullable
    private Object previousRequestStatus;

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return this.path;
    }

    public void setIgnoreNestedPath(boolean ignoreNestedPath) {
        this.ignoreNestedPath = ignoreNestedPath;
    }

    public boolean isIgnoreNestedPath() {
        return this.ignoreNestedPath;
    }

    @Override
    protected final int doStartTagInternal() throws Exception {
        String resolvedPath = getPath();
        if (!isIgnoreNestedPath()) {
            String nestedPath = (String) this.pageContext.getAttribute(NestedPathTag.NESTED_PATH_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
            // only prepend if not already an absolute path
            if (nestedPath != null && !resolvedPath.startsWith(nestedPath) && !resolvedPath.equals(nestedPath.substring(0, nestedPath.length() - 1))) {
                resolvedPath = nestedPath + resolvedPath;
            }
        }
        try {
            this.status = new BindStatus(getRequestContext(), resolvedPath, isHtmlEscape());
        } catch (IllegalStateException ex) {
            throw new JspTagException(ex.getMessage());
        }
        // Save previous status values, for re-exposure at the end of this tag.
        this.previousPageStatus = this.pageContext.getAttribute(STATUS_VARIABLE_NAME, PageContext.PAGE_SCOPE);
        this.previousRequestStatus = this.pageContext.getAttribute(STATUS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
        // Expose this tag's status object as PageContext attribute,
        // making it available for JSP EL.
        this.pageContext.removeAttribute(STATUS_VARIABLE_NAME, PageContext.PAGE_SCOPE);
        this.pageContext.setAttribute(STATUS_VARIABLE_NAME, this.status, PageContext.REQUEST_SCOPE);
        return EVAL_BODY_INCLUDE;
    }

    @Override
    public int doEndTag() {
        // Reset previous status values.
        if (this.previousPageStatus != null) {
            this.pageContext.setAttribute(STATUS_VARIABLE_NAME, this.previousPageStatus, PageContext.PAGE_SCOPE);
        }
        if (this.previousRequestStatus != null) {
            this.pageContext.setAttribute(STATUS_VARIABLE_NAME, this.previousRequestStatus, PageContext.REQUEST_SCOPE);
        } else {
            this.pageContext.removeAttribute(STATUS_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
        }
        return EVAL_PAGE;
    }

    private BindStatus getStatus() {
        Assert.state(this.status != null, "No current BindStatus");
        return this.status;
    }

    @Nullable
    public final String getProperty() {
        return getStatus().getExpression();
    }

    @Nullable
    public final Errors getErrors() {
        return getStatus().getErrors();
    }

    @Override
    @Nullable
    public final PropertyEditor getEditor() {
        return getStatus().getEditor();
    }

    @Override
    public void doFinally() {
        super.doFinally();
        this.status = null;
        this.previousPageStatus = null;
        this.previousRequestStatus = null;
    }

}
