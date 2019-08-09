package org.springframework.aop;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.lang.Nullable;

public interface ProxyMethodInvocation extends MethodInvocation {

    Object getProxy();

    MethodInvocation invocableClone();

    MethodInvocation invocableClone(Object... arguments);

    void setArguments(Object... arguments);

    void setUserAttribute(String key, @Nullable Object value);

    @Nullable
    Object getUserAttribute(String key);

}
