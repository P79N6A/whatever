package org.springframework.web.servlet.tags;

import org.springframework.lang.Nullable;
import org.springframework.web.util.TagUtils;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;
import java.beans.PropertyEditor;
import java.io.IOException;

@SuppressWarnings("serial")
public class TransformTag extends HtmlEscapingAwareTag {

    @Nullable
    private Object value;

    @Nullable
    private String var;

    private String scope = TagUtils.SCOPE_PAGE;

    public void setValue(Object value) {
        this.value = value;
    }

    public void setVar(String var) {
        this.var = var;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    @Override
    protected final int doStartTagInternal() throws JspException {
        if (this.value != null) {
            // Find the containing EditorAwareTag (e.g. BindTag), if applicable.
            EditorAwareTag tag = (EditorAwareTag) TagSupport.findAncestorWithClass(this, EditorAwareTag.class);
            if (tag == null) {
                throw new JspException("TransformTag can only be used within EditorAwareTag (e.g. BindTag)");
            }
            // OK, let's obtain the editor...
            String result = null;
            PropertyEditor editor = tag.getEditor();
            if (editor != null) {
                // If an editor was found, edit the value.
                editor.setValue(this.value);
                result = editor.getAsText();
            } else {
                // Else, just do a toString.
                result = this.value.toString();
            }
            result = htmlEscape(result);
            if (this.var != null) {
                this.pageContext.setAttribute(this.var, result, TagUtils.getScope(this.scope));
            } else {
                try {
                    // Else, just print it out.
                    this.pageContext.getOut().print(result);
                } catch (IOException ex) {
                    throw new JspException(ex);
                }
            }
        }
        return SKIP_BODY;
    }

}
