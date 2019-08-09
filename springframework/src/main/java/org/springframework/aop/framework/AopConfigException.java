package org.springframework.aop.framework;

import org.springframework.core.NestedRuntimeException;

@SuppressWarnings("serial")
public class AopConfigException extends NestedRuntimeException {

    public AopConfigException(String msg) {
        super(msg);
    }

    public AopConfigException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
