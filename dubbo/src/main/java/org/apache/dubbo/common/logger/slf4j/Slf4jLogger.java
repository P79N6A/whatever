package org.apache.dubbo.common.logger.slf4j;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.support.FailsafeLogger;
import org.slf4j.spi.LocationAwareLogger;

import java.io.Serializable;

public class Slf4jLogger implements Logger, Serializable {

    private static final long serialVersionUID = 1L;

    private static final String FQCN = FailsafeLogger.class.getName();

    private final org.slf4j.Logger logger;

    private final LocationAwareLogger locationAwareLogger;

    public Slf4jLogger(org.slf4j.Logger logger) {
        if (logger instanceof LocationAwareLogger) {
            locationAwareLogger = (LocationAwareLogger) logger;
        } else {
            locationAwareLogger = null;
        }
        this.logger = logger;
    }

    @Override
    public void trace(String msg) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.TRACE_INT, msg, null, null);
            return;
        }
        logger.trace(msg);
    }

    @Override
    public void trace(Throwable e) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.TRACE_INT, e.getMessage(), null, e);
            return;
        }
        logger.trace(e.getMessage(), e);
    }

    @Override
    public void trace(String msg, Throwable e) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.TRACE_INT, msg, null, e);
            return;
        }
        logger.trace(msg, e);
    }

    @Override
    public void debug(String msg) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.DEBUG_INT, msg, null, null);
            return;
        }
        logger.debug(msg);
    }

    @Override
    public void debug(Throwable e) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.DEBUG_INT, e.getMessage(), null, e);
            return;
        }
        logger.debug(e.getMessage(), e);
    }

    @Override
    public void debug(String msg, Throwable e) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.DEBUG_INT, msg, null, e);
            return;
        }
        logger.debug(msg, e);
    }

    @Override
    public void info(String msg) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.INFO_INT, msg, null, null);
            return;
        }
        logger.info(msg);
    }

    @Override
    public void info(Throwable e) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.INFO_INT, e.getMessage(), null, e);
            return;
        }
        logger.info(e.getMessage(), e);
    }

    @Override
    public void info(String msg, Throwable e) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.INFO_INT, msg, null, e);
            return;
        }
        logger.info(msg, e);
    }

    @Override
    public void warn(String msg) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.WARN_INT, msg, null, null);
            return;
        }
        logger.warn(msg);
    }

    @Override
    public void warn(Throwable e) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.WARN_INT, e.getMessage(), null, e);
            return;
        }
        logger.warn(e.getMessage(), e);
    }

    @Override
    public void warn(String msg, Throwable e) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.WARN_INT, msg, null, e);
            return;
        }
        logger.warn(msg, e);
    }

    @Override
    public void error(String msg) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.ERROR_INT, msg, null, null);
            return;
        }
        logger.error(msg);
    }

    @Override
    public void error(Throwable e) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.ERROR_INT, e.getMessage(), null, e);
            return;
        }
        logger.error(e.getMessage(), e);
    }

    @Override
    public void error(String msg, Throwable e) {
        if (locationAwareLogger != null) {
            locationAwareLogger.log(null, FQCN, LocationAwareLogger.ERROR_INT, msg, null, e);
            return;
        }
        logger.error(msg, e);
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

}
