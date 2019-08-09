package org.springframework.scheduling;

import org.springframework.core.NestedRuntimeException;

@SuppressWarnings("serial")
public class SchedulingException extends NestedRuntimeException {

    public SchedulingException(String msg) {
        super(msg);
    }

    public SchedulingException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
