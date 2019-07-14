package org.apache.dubbo.rpc.filter.tps;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.dubbo.common.constants.RpcConstants.*;

public class DefaultTPSLimiter implements TPSLimiter {

    private final ConcurrentMap<String, StatItem> stats = new ConcurrentHashMap<String, StatItem>();

    @Override
    public boolean isAllowable(URL url, Invocation invocation) {
        int rate = url.getParameter(TPS_LIMIT_RATE_KEY, -1);
        long interval = url.getParameter(TPS_LIMIT_INTERVAL_KEY, DEFAULT_TPS_LIMIT_INTERVAL);
        String serviceKey = url.getServiceKey();
        if (rate > 0) {
            StatItem statItem = stats.get(serviceKey);
            if (statItem == null) {
                stats.putIfAbsent(serviceKey, new StatItem(serviceKey, rate, interval));
                statItem = stats.get(serviceKey);
            } else {
                if (statItem.getRate() != rate || statItem.getInterval() != interval) {
                    stats.put(serviceKey, new StatItem(serviceKey, rate, interval));
                    statItem = stats.get(serviceKey);
                }
            }
            return statItem.isAllowable();
        } else {
            StatItem statItem = stats.get(serviceKey);
            if (statItem != null) {
                stats.remove(serviceKey);
            }
        }
        return true;
    }

}
