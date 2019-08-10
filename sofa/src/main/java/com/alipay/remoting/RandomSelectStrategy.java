package com.alipay.remoting;

import com.alipay.remoting.config.Configs;
import com.alipay.remoting.config.switches.GlobalSwitch;
import com.alipay.remoting.log.BoltLoggerFactory;
import com.alipay.remoting.util.StringUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomSelectStrategy implements ConnectionSelectStrategy {

    private static final Logger logger = BoltLoggerFactory.getLogger("CommonDefault");

    private static final int MAX_TIMES = 5;

    private final Random random = new Random();

    private final GlobalSwitch globalSwitch;

    public RandomSelectStrategy(GlobalSwitch globalSwitch) {
        this.globalSwitch = globalSwitch;
    }

    @Override
    public Connection select(List<Connection> connections) {
        try {
            if (connections == null) {
                return null;
            }
            int size = connections.size();
            if (size == 0) {
                return null;
            }
            Connection result;
            if (null != this.globalSwitch && this.globalSwitch.isOn(GlobalSwitch.CONN_MONITOR_SWITCH)) {
                List<Connection> serviceStatusOnConnections = new ArrayList<Connection>();
                for (Connection conn : connections) {
                    String serviceStatus = (String) conn.getAttribute(Configs.CONN_SERVICE_STATUS);
                    if (!StringUtils.equals(serviceStatus, Configs.CONN_SERVICE_STATUS_OFF)) {
                        serviceStatusOnConnections.add(conn);
                    }
                }
                if (serviceStatusOnConnections.size() == 0) {
                    throw new Exception("No available connection when select in RandomSelectStrategy.");
                }
                result = randomGet(serviceStatusOnConnections);
            } else {
                result = randomGet(connections);
            }
            return result;
        } catch (Throwable e) {
            logger.error("Choose connection failed using RandomSelectStrategy!", e);
            return null;
        }
    }

    private Connection randomGet(List<Connection> connections) {
        if (null == connections || connections.isEmpty()) {
            return null;
        }
        int size = connections.size();
        int tries = 0;
        Connection result = null;
        while ((result == null || !result.isFine()) && tries++ < MAX_TIMES) {
            result = connections.get(this.random.nextInt(size));
        }
        if (result != null && !result.isFine()) {
            result = null;
        }
        return result;
    }

}
