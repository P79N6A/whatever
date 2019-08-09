package org.springframework.beans;

import org.springframework.lang.Nullable;

@SuppressWarnings("serial")
public class FatalBeanException extends BeansException {

    public FatalBeanException(String msg) {
        super(msg);
    }

    public FatalBeanException(String msg, @Nullable Throwable cause) {
        super(msg, cause);
    }

}
