package org.springframework.web.servlet.tags;

import org.springframework.lang.Nullable;

import javax.servlet.jsp.JspTagException;

public interface ArgumentAware {

    void addArgument(@Nullable Object argument) throws JspTagException;

}
