package org.apache.dubbo.rpc.filter;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.support.AccessLogData;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.common.constants.CommonConstants.*;
import static org.apache.dubbo.common.constants.RpcConstants.ACCESS_LOG_KEY;

@Activate(group = PROVIDER, value = ACCESS_LOG_KEY)
public class AccessLogFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(AccessLogFilter.class);

    private static final String ACCESS_LOG_KEY = "dubbo.accesslog";

    private static final int LOG_MAX_BUFFER = 5000;

    private static final long LOG_OUTPUT_INTERVAL = 5000;

    private static final String FILE_DATE_FORMAT = "yyyyMMdd";

    private static final DateFormat FILE_NAME_FORMATTER = new SimpleDateFormat(FILE_DATE_FORMAT);

    private static final Map<String, Set<AccessLogData>> LOG_ENTRIES = new ConcurrentHashMap<String, Set<AccessLogData>>();

    private static final ScheduledExecutorService LOG_SCHEDULED = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Dubbo-Access-Log", true));

    public AccessLogFilter() {
        LOG_SCHEDULED.scheduleWithFixedDelay(this::writeLogToFile, LOG_OUTPUT_INTERVAL, LOG_OUTPUT_INTERVAL, TimeUnit.MILLISECONDS);
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation inv) throws RpcException {


        try {
            String accessLogKey = invoker.getUrl().getParameter(ACCESS_LOG_KEY);
            if (ConfigUtils.isNotEmpty(accessLogKey)) {
                AccessLogData logData = buildAccessLogData(invoker, inv);
                log(accessLogKey, logData);
            }
        } catch (Throwable t) {
            logger.warn("Exception in AccessLogFilter of service(" + invoker + " -> " + inv + ")", t);
        }
        return invoker.invoke(inv);
    }

    private void log(String accessLog, AccessLogData accessLogData) {
        Set<AccessLogData> logSet = LOG_ENTRIES.computeIfAbsent(accessLog, k -> new ConcurrentHashSet<>());
        if (logSet.size() < LOG_MAX_BUFFER) {
            logSet.add(accessLogData);
        } else {
            logger.warn("AccessLog buffer is full skipping buffer ");
        }
    }

    private void writeLogToFile() {
        if (!LOG_ENTRIES.isEmpty()) {
            for (Map.Entry<String, Set<AccessLogData>> entry : LOG_ENTRIES.entrySet()) {
                try {
                    String accessLog = entry.getKey();
                    Set<AccessLogData> logSet = entry.getValue();
                    if (ConfigUtils.isDefault(accessLog)) {
                        processWithServiceLogger(logSet);
                    } else {
                        File file = new File(accessLog);
                        createIfLogDirAbsent(file);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Append log to " + accessLog);
                        }
                        renameFile(file);
                        processWithAccessKeyLogger(logSet, file);
                    }

                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    private void processWithAccessKeyLogger(Set<AccessLogData> logSet, File file) throws IOException {
        try (FileWriter writer = new FileWriter(file, true)) {
            for (Iterator<AccessLogData> iterator = logSet.iterator(); iterator.hasNext(); iterator.remove()) {
                writer.write(iterator.next().getLogMessage());
                writer.write("\r\n");
            }
            writer.flush();
        }
    }

    private AccessLogData buildAccessLogData(Invoker<?> invoker, Invocation inv) {
        RpcContext context = RpcContext.getContext();
        AccessLogData logData = AccessLogData.newLogData();
        logData.setServiceName(invoker.getInterface().getName());
        logData.setMethodName(inv.getMethodName());
        logData.setVersion(invoker.getUrl().getParameter(VERSION_KEY));
        logData.setGroup(invoker.getUrl().getParameter(GROUP_KEY));
        logData.setInvocationTime(new Date());
        logData.setTypes(inv.getParameterTypes());
        logData.setArguments(inv.getArguments());
        return logData;
    }

    private void processWithServiceLogger(Set<AccessLogData> logSet) {
        for (Iterator<AccessLogData> iterator = logSet.iterator(); iterator.hasNext(); iterator.remove()) {
            AccessLogData logData = iterator.next();
            LoggerFactory.getLogger(ACCESS_LOG_KEY + "." + logData.getServiceName()).info(logData.getLogMessage());
        }
    }

    private void createIfLogDirAbsent(File file) {
        File dir = file.getParentFile();
        if (null != dir && !dir.exists()) {
            dir.mkdirs();
        }
    }

    private void renameFile(File file) {
        if (file.exists()) {
            String now = FILE_NAME_FORMATTER.format(new Date());
            String last = FILE_NAME_FORMATTER.format(new Date(file.lastModified()));
            if (!now.equals(last)) {
                File archive = new File(file.getAbsolutePath() + "." + last);
                file.renameTo(archive);
            }
        }
    }

}
