package org.springframework.aop.interceptor;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.lang.Nullable;

import java.lang.reflect.Method;

@SuppressWarnings("serial")
public abstract class AbstractMonitoringInterceptor extends AbstractTraceInterceptor {

    private String prefix = "";

    private String suffix = "";

    private boolean logTargetClassInvocation = false;

    public void setPrefix(@Nullable String prefix) {
        this.prefix = (prefix != null ? prefix : "");
    }

    protected String getPrefix() {
        return this.prefix;
    }

    public void setSuffix(@Nullable String suffix) {
        this.suffix = (suffix != null ? suffix : "");
    }

    protected String getSuffix() {
        return this.suffix;
    }

    public void setLogTargetClassInvocation(boolean logTargetClassInvocation) {
        this.logTargetClassInvocation = logTargetClassInvocation;
    }

    protected String createInvocationTraceName(MethodInvocation invocation) {
        Method method = invocation.getMethod();
        Class<?> clazz = method.getDeclaringClass();
        if (this.logTargetClassInvocation && clazz.isInstance(invocation.getThis())) {
            clazz = invocation.getThis().getClass();
        }
        return getPrefix() + clazz.getName() + '.' + method.getName() + getSuffix();
    }

}
