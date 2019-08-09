package org.springframework.web.bind;

import org.springframework.web.util.NestedServletException;

@SuppressWarnings("serial")
public class ServletRequestBindingException extends NestedServletException {

    public ServletRequestBindingException(String msg) {
        super(msg);
    }

    public ServletRequestBindingException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
