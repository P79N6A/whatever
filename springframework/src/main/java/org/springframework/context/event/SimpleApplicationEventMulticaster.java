package org.springframework.context.event;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.ErrorHandler;

import java.util.concurrent.Executor;

public class SimpleApplicationEventMulticaster extends AbstractApplicationEventMulticaster {

    @Nullable
    private Executor taskExecutor;

    @Nullable
    private ErrorHandler errorHandler;

    public SimpleApplicationEventMulticaster() {
    }

    public SimpleApplicationEventMulticaster(BeanFactory beanFactory) {
        setBeanFactory(beanFactory);
    }

    public void setTaskExecutor(@Nullable Executor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    @Nullable
    protected Executor getTaskExecutor() {
        return this.taskExecutor;
    }

    public void setErrorHandler(@Nullable ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    @Nullable
    protected ErrorHandler getErrorHandler() {
        return this.errorHandler;
    }

    @Override
    public void multicastEvent(ApplicationEvent event) {
        multicastEvent(event, resolveDefaultEventType(event));
    }

    @Override
    public void multicastEvent(final ApplicationEvent event, @Nullable ResolvableType eventType) {
        ResolvableType type = (eventType != null ? eventType : resolveDefaultEventType(event));
        Executor executor = getTaskExecutor();
        for (ApplicationListener<?> listener : getApplicationListeners(event, type)) {
            if (executor != null) {
                executor.execute(() -> invokeListener(listener, event));
            } else {
                invokeListener(listener, event);
            }
        }
    }

    private ResolvableType resolveDefaultEventType(ApplicationEvent event) {
        return ResolvableType.forInstance(event);
    }

    protected void invokeListener(ApplicationListener<?> listener, ApplicationEvent event) {
        ErrorHandler errorHandler = getErrorHandler();
        if (errorHandler != null) {
            try {
                doInvokeListener(listener, event);
            } catch (Throwable err) {
                errorHandler.handleError(err);
            }
        } else {
            doInvokeListener(listener, event);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void doInvokeListener(ApplicationListener listener, ApplicationEvent event) {
        try {
            listener.onApplicationEvent(event);
        } catch (ClassCastException ex) {
            String msg = ex.getMessage();
            if (msg == null || matchesClassCastMessage(msg, event.getClass())) {
                // Possibly a lambda-defined listener which we could not resolve the generic event type for
                // -> let's suppress the exception and just log a debug message.
                Log logger = LogFactory.getLog(getClass());
                if (logger.isTraceEnabled()) {
                    logger.trace("Non-matching event type for listener: " + listener, ex);
                }
            } else {
                throw ex;
            }
        }
    }

    private boolean matchesClassCastMessage(String classCastMessage, Class<?> eventClass) {
        // On Java 8, the message starts with the class name: "java.lang.String cannot be cast..."
        if (classCastMessage.startsWith(eventClass.getName())) {
            return true;
        }
        // On Java 11, the message starts with "class ..." a.k.a. Class.toString()
        if (classCastMessage.startsWith(eventClass.toString())) {
            return true;
        }
        // On Java 9, the message used to contain the module name: "java.base/java.lang.String cannot be cast..."
        int moduleSeparatorIndex = classCastMessage.indexOf('/');
        if (moduleSeparatorIndex != -1 && classCastMessage.startsWith(eventClass.getName(), moduleSeparatorIndex + 1)) {
            return true;
        }
        // Assuming an unrelated class cast failure...
        return false;
    }

}
