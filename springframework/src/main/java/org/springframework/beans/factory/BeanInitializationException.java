package org.springframework.beans.factory;

import org.springframework.beans.FatalBeanException;

@SuppressWarnings("serial")
public class BeanInitializationException extends FatalBeanException {

    public BeanInitializationException(String msg) {
        super(msg);
    }

    public BeanInitializationException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
