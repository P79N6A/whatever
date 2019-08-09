package org.springframework.web.servlet.tags;

import org.springframework.lang.Nullable;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

@SuppressWarnings("serial")
public class ParamTag extends BodyTagSupport {

    private String name = "";

    @Nullable
    private String value;

    private boolean valueSet;

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(String value) {
        this.value = value;
        this.valueSet = true;
    }

    @Override
    public int doEndTag() throws JspException {
        Param param = new Param();
        param.setName(this.name);
        if (this.valueSet) {
            param.setValue(this.value);
        } else if (getBodyContent() != null) {
            // Get the value from the tag body
            param.setValue(getBodyContent().getString().trim());
        }
        // Find a param aware ancestor
        ParamAware paramAwareTag = (ParamAware) findAncestorWithClass(this, ParamAware.class);
        if (paramAwareTag == null) {
            throw new JspException("The param tag must be a descendant of a tag that supports parameters");
        }
        paramAwareTag.addParam(param);
        return EVAL_PAGE;
    }

    @Override
    public void release() {
        super.release();
        this.name = "";
        this.value = null;
        this.valueSet = false;
    }

}
