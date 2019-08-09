package org.springframework.jndi;

import org.springframework.core.NestedRuntimeException;

import javax.naming.NamingException;

@SuppressWarnings("serial")
public class JndiLookupFailureException extends NestedRuntimeException {

    public JndiLookupFailureException(String msg, NamingException cause) {
        super(msg, cause);
    }

}
