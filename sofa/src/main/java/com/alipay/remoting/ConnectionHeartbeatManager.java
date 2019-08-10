package com.alipay.remoting;

public interface ConnectionHeartbeatManager {

    void disableHeartbeat(Connection connection);

    void enableHeartbeat(Connection connection);

}
