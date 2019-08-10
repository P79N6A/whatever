package com.alipay.remoting;

import com.alipay.remoting.config.ConfigManager;
import com.alipay.remoting.log.BoltLoggerFactory;
import com.alipay.remoting.util.RunStateRecordedFutureTask;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DefaultConnectionMonitor extends AbstractLifeCycle {

    private static final Logger logger = BoltLoggerFactory.getLogger("CommonDefault");

    private final DefaultConnectionManager connectionManager;

    private final ConnectionMonitorStrategy strategy;

    private ScheduledThreadPoolExecutor executor;

    public DefaultConnectionMonitor(ConnectionMonitorStrategy strategy, DefaultConnectionManager connectionManager) {
        if (strategy == null) {
            throw new IllegalArgumentException("null strategy");
        }
        if (connectionManager == null) {
            throw new IllegalArgumentException("null connectionManager");
        }
        this.strategy = strategy;
        this.connectionManager = connectionManager;
    }

    @Override
    public void startup() throws LifeCycleException {
        super.startup();
        long initialDelay = ConfigManager.conn_monitor_initial_delay();
        long period = ConfigManager.conn_monitor_period();
        this.executor = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("ConnectionMonitorThread", true), new ThreadPoolExecutor.AbortPolicy());
        this.executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    Map<String, RunStateRecordedFutureTask<ConnectionPool>> connPools = connectionManager.getConnPools();
                    strategy.monitor(connPools);
                } catch (Exception e) {
                    logger.warn("MonitorTask error", e);
                }
            }
        }, initialDelay, period, TimeUnit.MILLISECONDS);
    }

    @Override
    public void shutdown() throws LifeCycleException {
        super.shutdown();
        executor.purge();
        executor.shutdown();
    }

    @Deprecated
    public void start() {
        startup();
    }

    @Deprecated
    public void destroy() {
        shutdown();
    }

}
