package org.springframework.scheduling.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.ErrorHandler;
import org.springframework.util.ReflectionUtils;

public abstract class TaskUtils {

    public static final ErrorHandler LOG_AND_SUPPRESS_ERROR_HANDLER = new LoggingErrorHandler();

    public static final ErrorHandler LOG_AND_PROPAGATE_ERROR_HANDLER = new PropagatingErrorHandler();

    public static DelegatingErrorHandlingRunnable decorateTaskWithErrorHandler(Runnable task, @Nullable ErrorHandler errorHandler, boolean isRepeatingTask) {
        if (task instanceof DelegatingErrorHandlingRunnable) {
            return (DelegatingErrorHandlingRunnable) task;
        }
        ErrorHandler eh = (errorHandler != null ? errorHandler : getDefaultErrorHandler(isRepeatingTask));
        return new DelegatingErrorHandlingRunnable(task, eh);
    }

    public static ErrorHandler getDefaultErrorHandler(boolean isRepeatingTask) {
        return (isRepeatingTask ? LOG_AND_SUPPRESS_ERROR_HANDLER : LOG_AND_PROPAGATE_ERROR_HANDLER);
    }

    private static class LoggingErrorHandler implements ErrorHandler {

        private final Log logger = LogFactory.getLog(LoggingErrorHandler.class);

        @Override
        public void handleError(Throwable t) {
            if (logger.isErrorEnabled()) {
                logger.error("Unexpected error occurred in scheduled task.", t);
            }
        }

    }

    private static class PropagatingErrorHandler extends LoggingErrorHandler {

        @Override
        public void handleError(Throwable t) {
            super.handleError(t);
            ReflectionUtils.rethrowRuntimeException(t);
        }

    }

}
