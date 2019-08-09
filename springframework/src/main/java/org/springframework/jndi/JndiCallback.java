package org.springframework.jndi;

import org.springframework.lang.Nullable;

import javax.naming.Context;
import javax.naming.NamingException;

@FunctionalInterface
public interface JndiCallback<T> {

    @Nullable
    T doInContext(Context ctx) throws NamingException;

}

