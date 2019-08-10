package com.alipay.remoting;

import com.alipay.remoting.util.RunStateRecordedFutureTask;

import java.util.List;
import java.util.Map;

public interface ConnectionMonitorStrategy {

    @Deprecated
    Map<String, List<Connection>> filter(List<Connection> connections);

    void monitor(Map<String, RunStateRecordedFutureTask<ConnectionPool>> connPools);

}
