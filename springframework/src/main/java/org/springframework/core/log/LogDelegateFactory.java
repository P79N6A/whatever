package org.springframework.core.log;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LogDelegateFactory {

    private LogDelegateFactory() {
    }

    public static Log getCompositeLog(Log primaryLogger, Log secondaryLogger, Log... tertiaryLoggers) {
        List<Log> loggers = new ArrayList<>(2 + tertiaryLoggers.length);
        loggers.add(primaryLogger);
        loggers.add(secondaryLogger);
        Collections.addAll(loggers, tertiaryLoggers);
        return new CompositeLog(loggers);
    }

    public static Log getHiddenLog(Class<?> clazz) {
        return LogFactory.getLog("_" + clazz.getName());
    }

}
