package org.springframework.web.servlet.tags.form;

import org.springframework.beans.PropertyAccessor;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.BindStatus;
import org.springframework.web.servlet.support.RequestDataValueProcessor;
import org.springframework.web.servlet.tags.EditorAwareTag;
import org.springframework.web.servlet.tags.NestedPathTag;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import java.beans.PropertyEditor;

@SuppressWarnings("serial")
public abstract class AbstractDataBoundFormElementTag extends AbstractFormTag implements EditorAwareTag {

    protected static final String NESTED_PATH_VARIABLE_NAME = NestedPathTag.NESTED_PATH_VARIABLE_NAME;

    @Nullable
    private String path;

    @Nullable
    private String id;

    @Nullable
    private BindStatus bindStatus;

    public void setPath(String path) {
        this.path = path;
    }

    protected final String getPath() throws JspException {
        String resolvedPath = (String) evaluate("path", this.path);
        return (resolvedPath != null ? resolvedPath : "");
    }

    @Override
    public void setId(@Nullable String id) {
        this.id = id;
    }

    @Override
    @Nullable
    public String getId() {
        return this.id;
    }

    protected void writeDefaultAttributes(TagWriter tagWriter) throws JspException {
        writeOptionalAttribute(tagWriter, "id", resolveId());
        writeOptionalAttribute(tagWriter, "name", getName());
    }

    @Nullable
    protected String resolveId() throws JspException {
        Object id = evaluate("id", getId());
        if (id != null) {
            String idString = id.toString();
            return (StringUtils.hasText(idString) ? idString : null);
        }
        return autogenerateId();
    }

    @Nullable
    protected String autogenerateId() throws JspException {
        String name = getName();
        return (name != null ? StringUtils.deleteAny(name, "[]") : null);
    }

    @Nullable
    protected String getName() throws JspException {
        return getPropertyPath();
    }

    protected BindStatus getBindStatus() throws JspException {
        if (this.bindStatus == null) {
            // HTML escaping in tags is performed by the ValueFormatter class.
            String nestedPath = getNestedPath();
            String pathToUse = (nestedPath != null ? nestedPath + getPath() : getPath());
            if (pathToUse.endsWith(PropertyAccessor.NESTED_PROPERTY_SEPARATOR)) {
                pathToUse = pathToUse.substring(0, pathToUse.length() - 1);
            }
            this.bindStatus = new BindStatus(getRequestContext(), pathToUse, false);
        }
        return this.bindStatus;
    }

    @Nullable
    protected String getNestedPath() {
        return (String) this.pageContext.getAttribute(NESTED_PATH_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
    }

    protected String getPropertyPath() throws JspException {
        String expression = getBindStatus().getExpression();
        return (expression != null ? expression : "");
    }

    @Nullable
    protected final Object getBoundValue() throws JspException {
        return getBindStatus().getValue();
    }

    @Nullable
    protected PropertyEditor getPropertyEditor() throws JspException {
        return getBindStatus().getEditor();
    }

    @Override
    @Nullable
    public final PropertyEditor getEditor() throws JspException {
        return getPropertyEditor();
    }

    protected String convertToDisplayString(@Nullable Object value) throws JspException {
        PropertyEditor editor = (value != null ? getBindStatus().findEditor(value.getClass()) : null);
        return getDisplayString(value, editor);
    }

    protected final String processFieldValue(@Nullable String name, String value, String type) {
        RequestDataValueProcessor processor = getRequestContext().getRequestDataValueProcessor();
        ServletRequest request = this.pageContext.getRequest();
        if (processor != null && request instanceof HttpServletRequest) {
            value = processor.processFormFieldValue((HttpServletRequest) request, name, value, type);
        }
        return value;
    }

    @Override
    public void doFinally() {
        super.doFinally();
        this.bindStatus = null;
    }

}
