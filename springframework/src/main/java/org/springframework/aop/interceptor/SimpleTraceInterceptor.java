package org.springframework.aop.interceptor;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;

@SuppressWarnings("serial")
public class SimpleTraceInterceptor extends AbstractTraceInterceptor {

    public SimpleTraceInterceptor() {
    }

    public SimpleTraceInterceptor(boolean useDynamicLogger) {
        setUseDynamicLogger(useDynamicLogger);
    }

    @Override
    protected Object invokeUnderTrace(MethodInvocation invocation, Log logger) throws Throwable {
        String invocationDescription = getInvocationDescription(invocation);
        writeToLog(logger, "Entering " + invocationDescription);
        try {
            Object rval = invocation.proceed();
            writeToLog(logger, "Exiting " + invocationDescription);
            return rval;
        } catch (Throwable ex) {
            writeToLog(logger, "Exception thrown in " + invocationDescription, ex);
            throw ex;
        }
    }

    protected String getInvocationDescription(MethodInvocation invocation) {
        return "method '" + invocation.getMethod().getName() + "' of class [" + invocation.getThis().getClass().getName() + "]";
    }

}
