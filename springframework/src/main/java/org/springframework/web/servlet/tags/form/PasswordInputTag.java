package org.springframework.web.servlet.tags.form;

import javax.servlet.jsp.JspException;

@SuppressWarnings("serial")
public class PasswordInputTag extends InputTag {

    private boolean showPassword = false;

    public void setShowPassword(boolean showPassword) {
        this.showPassword = showPassword;
    }

    public boolean isShowPassword() {
        return this.showPassword;
    }

    @Override
    protected boolean isValidDynamicAttribute(String localName, Object value) {
        return !"type".equals(localName);
    }

    @Override
    protected String getType() {
        return "password";
    }

    @Override
    protected void writeValue(TagWriter tagWriter) throws JspException {
        if (this.showPassword) {
            super.writeValue(tagWriter);
        } else {
            tagWriter.writeAttribute("value", processFieldValue(getName(), "", getType()));
        }
    }

}
