package org.springframework.aop.interceptor;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.springframework.util.StopWatch;

@SuppressWarnings("serial")
public class PerformanceMonitorInterceptor extends AbstractMonitoringInterceptor {

    public PerformanceMonitorInterceptor() {
    }

    public PerformanceMonitorInterceptor(boolean useDynamicLogger) {
        setUseDynamicLogger(useDynamicLogger);
    }

    @Override
    protected Object invokeUnderTrace(MethodInvocation invocation, Log logger) throws Throwable {
        String name = createInvocationTraceName(invocation);
        StopWatch stopWatch = new StopWatch(name);
        stopWatch.start(name);
        try {
            return invocation.proceed();
        } finally {
            stopWatch.stop();
            writeToLog(logger, stopWatch.shortSummary());
        }
    }

}
