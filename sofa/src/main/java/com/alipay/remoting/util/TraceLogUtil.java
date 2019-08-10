package com.alipay.remoting.util;

import com.alipay.remoting.InvokeContext;
import org.slf4j.Logger;

public class TraceLogUtil {

    public static void printConnectionTraceLog(Logger logger, String traceId, InvokeContext invokeContext) {
        String sourceIp = invokeContext.get(InvokeContext.CLIENT_LOCAL_IP);
        Integer sourcePort = invokeContext.get(InvokeContext.CLIENT_LOCAL_PORT);
        String targetIp = invokeContext.get(InvokeContext.CLIENT_REMOTE_IP);
        Integer targetPort = invokeContext.get(InvokeContext.CLIENT_REMOTE_PORT);
        StringBuilder logMsg = new StringBuilder();
        logMsg.append(traceId).append(",");
        logMsg.append(sourceIp).append(",");
        logMsg.append(sourcePort).append(",");
        logMsg.append(targetIp).append(",");
        logMsg.append(targetPort);
        if (logger.isInfoEnabled()) {
            logger.info(logMsg.toString());
        }
    }

}