package com.alipay.remoting;

import com.alipay.remoting.config.ConfigManager;
import com.alipay.remoting.config.Configs;
import com.alipay.remoting.log.BoltLoggerFactory;
import com.alipay.remoting.util.FutureTaskUtil;
import com.alipay.remoting.util.RemotingUtil;
import com.alipay.remoting.util.RunStateRecordedFutureTask;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class ScheduledDisconnectStrategy implements ConnectionMonitorStrategy {
    private static final Logger logger = BoltLoggerFactory.getLogger("CommonDefault");

    private final int connectionThreshold;

    private final Random random;

    public ScheduledDisconnectStrategy() {
        this.connectionThreshold = ConfigManager.conn_threshold();
        this.random = new Random();
    }

    @Deprecated
    @Override
    public Map<String, List<Connection>> filter(List<Connection> connections) {
        List<Connection> serviceOnConnections = new ArrayList<Connection>();
        List<Connection> serviceOffConnections = new ArrayList<Connection>();
        Map<String, List<Connection>> filteredConnections = new ConcurrentHashMap<String, List<Connection>>();
        for (Connection connection : connections) {
            if (isConnectionOn(connection)) {
                serviceOnConnections.add(connection);
            } else {
                serviceOffConnections.add(connection);
            }
        }
        filteredConnections.put(Configs.CONN_SERVICE_STATUS_ON, serviceOnConnections);
        filteredConnections.put(Configs.CONN_SERVICE_STATUS_OFF, serviceOffConnections);
        return filteredConnections;
    }

    @Override
    public void monitor(Map<String, RunStateRecordedFutureTask<ConnectionPool>> connPools) {
        try {
            if (connPools == null || connPools.size() == 0) {
                return;
            }
            for (Map.Entry<String, RunStateRecordedFutureTask<ConnectionPool>> entry : connPools.entrySet()) {
                String poolKey = entry.getKey();
                ConnectionPool pool = FutureTaskUtil.getFutureTaskResult(entry.getValue(), logger);
                List<Connection> serviceOnConnections = new ArrayList<Connection>();
                List<Connection> serviceOffConnections = new ArrayList<Connection>();
                for (Connection connection : pool.getAll()) {
                    if (isConnectionOn(connection)) {
                        serviceOnConnections.add(connection);
                    } else {
                        serviceOffConnections.add(connection);
                    }
                }
                if (serviceOnConnections.size() > connectionThreshold) {
                    Connection freshSelectConnect = serviceOnConnections.get(random.nextInt(serviceOnConnections.size()));
                    freshSelectConnect.setAttribute(Configs.CONN_SERVICE_STATUS, Configs.CONN_SERVICE_STATUS_OFF);
                    serviceOffConnections.add(freshSelectConnect);
                } else {
                    if (logger.isInfoEnabled()) {
                        logger.info("serviceOnConnections({}) size[{}], CONNECTION_THRESHOLD[{}].", poolKey, serviceOnConnections.size(), connectionThreshold);
                    }
                }
                for (Connection offConn : serviceOffConnections) {
                    if (offConn.isInvokeFutureMapFinish()) {
                        if (offConn.isFine()) {
                            offConn.close();
                        }
                    } else {
                        if (logger.isInfoEnabled()) {
                            logger.info("Address={} won't close at this schedule turn", RemotingUtil.parseRemoteAddress(offConn.getChannel()));
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("ScheduledDisconnectStrategy monitor error", e);
        }
    }

    private boolean isConnectionOn(Connection connection) {
        String serviceStatus = (String) connection.getAttribute(Configs.CONN_SERVICE_STATUS);
        return serviceStatus == null || Boolean.parseBoolean(serviceStatus);
    }

}
