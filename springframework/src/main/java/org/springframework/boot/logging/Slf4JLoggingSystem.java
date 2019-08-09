package org.springframework.boot.logging;

import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public abstract class Slf4JLoggingSystem extends AbstractLoggingSystem {

    private static final String BRIDGE_HANDLER = "org.slf4j.bridge.SLF4JBridgeHandler";

    public Slf4JLoggingSystem(ClassLoader classLoader) {
        super(classLoader);
    }

    @Override
    public void beforeInitialize() {
        super.beforeInitialize();
        configureJdkLoggingBridgeHandler();
    }

    @Override
    public void cleanUp() {
        if (isBridgeHandlerAvailable()) {
            removeJdkLoggingBridgeHandler();
        }
    }

    @Override
    protected void loadConfiguration(LoggingInitializationContext initializationContext, String location, LogFile logFile) {
        Assert.notNull(location, "Location must not be null");
        if (initializationContext != null) {
            applySystemProperties(initializationContext.getEnvironment(), logFile);
        }
    }

    private void configureJdkLoggingBridgeHandler() {
        try {
            if (isBridgeJulIntoSlf4j()) {
                removeJdkLoggingBridgeHandler();
                SLF4JBridgeHandler.install();
            }
        } catch (Throwable ex) {
            // Ignore. No java.util.logging bridge is installed.
        }
    }

    protected final boolean isBridgeJulIntoSlf4j() {
        return isBridgeHandlerAvailable() && isJulUsingASingleConsoleHandlerAtMost();
    }

    protected final boolean isBridgeHandlerAvailable() {
        return ClassUtils.isPresent(BRIDGE_HANDLER, getClassLoader());
    }

    private boolean isJulUsingASingleConsoleHandlerAtMost() {
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        Handler[] handlers = rootLogger.getHandlers();
        return handlers.length == 0 || (handlers.length == 1 && handlers[0] instanceof ConsoleHandler);
    }

    private void removeJdkLoggingBridgeHandler() {
        try {
            removeDefaultRootHandler();
            SLF4JBridgeHandler.uninstall();
        } catch (Throwable ex) {
            // Ignore and continue
        }
    }

    private void removeDefaultRootHandler() {
        try {
            Logger rootLogger = LogManager.getLogManager().getLogger("");
            Handler[] handlers = rootLogger.getHandlers();
            if (handlers.length == 1 && handlers[0] instanceof ConsoleHandler) {
                rootLogger.removeHandler(handlers[0]);
            }
        } catch (Throwable ex) {
            // Ignore and continue
        }
    }

}
