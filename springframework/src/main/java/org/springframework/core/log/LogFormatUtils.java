package org.springframework.core.log;

import org.apache.commons.logging.Log;
import org.springframework.lang.Nullable;

import java.util.function.Function;

public abstract class LogFormatUtils {

    public static String formatValue(@Nullable Object value, boolean limitLength) {
        if (value == null) {
            return "";
        }
        String str;
        if (value instanceof CharSequence) {
            str = "\"" + value + "\"";
        } else {
            try {
                str = value.toString();
            } catch (Throwable ex) {
                str = ex.toString();
            }
        }
        return (limitLength && str.length() > 100 ? str.substring(0, 100) + " (truncated)..." : str);
    }

    public static void traceDebug(Log logger, Function<Boolean, String> messageFactory) {
        if (logger.isDebugEnabled()) {
            boolean traceEnabled = logger.isTraceEnabled();
            String logMessage = messageFactory.apply(traceEnabled);
            if (traceEnabled) {
                logger.trace(logMessage);
            } else {
                logger.debug(logMessage);
            }
        }
    }

}
