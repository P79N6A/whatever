package org.springframework.web.servlet.tags;

import org.springframework.lang.Nullable;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

@SuppressWarnings("serial")
public class ArgumentTag extends BodyTagSupport {

    @Nullable
    private Object value;

    private boolean valueSet;

    public void setValue(Object value) {
        this.value = value;
        this.valueSet = true;
    }

    @Override
    public int doEndTag() throws JspException {
        Object argument = null;
        if (this.valueSet) {
            argument = this.value;
        } else if (getBodyContent() != null) {
            // Get the value from the tag body
            argument = getBodyContent().getString().trim();
        }
        // Find a param-aware ancestor
        ArgumentAware argumentAwareTag = (ArgumentAware) findAncestorWithClass(this, ArgumentAware.class);
        if (argumentAwareTag == null) {
            throw new JspException("The argument tag must be a descendant of a tag that supports arguments");
        }
        argumentAwareTag.addArgument(argument);
        return EVAL_PAGE;
    }

    @Override
    public void release() {
        super.release();
        this.value = null;
        this.valueSet = false;
    }

}
