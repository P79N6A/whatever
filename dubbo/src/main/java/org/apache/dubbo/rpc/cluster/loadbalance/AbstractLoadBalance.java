package org.apache.dubbo.rpc.cluster.loadbalance;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.LoadBalance;

import java.util.List;

import static org.apache.dubbo.rpc.cluster.Constants.*;

public abstract class AbstractLoadBalance implements LoadBalance {

    /**
     * 保证当服务运行时长小于服务预热时间时，对服务进行降权，避免让服务在启动之初就处于高负载状态
     * 主要目的是让服务启动后“低功率”运行一段时间，使其效率慢慢提升至最佳状态
     */
    static int calculateWarmupWeight(int uptime, int warmup, int weight) {
        // 计算权重，下面代码逻辑上形似于 (uptime / warmup) * weight
        // 随着服务运行时间uptime增大，权重计算值ww会慢慢接近配置值weight
        int ww = (int) ((float) uptime / ((float) warmup / (float) weight));
        return ww < 1 ? 1 : (ww > weight ? weight : ww);
    }

    @Override
    public <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        if (CollectionUtils.isEmpty(invokers)) {
            return null;
        }
        // 如果仅有一个Invoker，直接返回，无需负载均衡
        if (invokers.size() == 1) {
            return invokers.get(0);
        }
        // 负载均衡
        return doSelect(invokers, url, invocation);
    }

    protected abstract <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation);

    protected int getWeight(Invoker<?> invoker, Invocation invocation) {
        // 从url中获取权重weight配置值
        int weight = invoker.getUrl().getMethodParameter(invocation.getMethodName(), WEIGHT_KEY, DEFAULT_WEIGHT);
        if (weight > 0) {
            // 获取服务提供者启动时间戳
            long timestamp = invoker.getUrl().getParameter(REMOTE_TIMESTAMP_KEY, 0L);
            if (timestamp > 0L) {
                // 计算服务提供者运行时长
                int uptime = (int) (System.currentTimeMillis() - timestamp);
                // 获取服务预热时间，默认为10分钟
                int warmup = invoker.getUrl().getParameter(WARMUP_KEY, DEFAULT_WARMUP);
                // 如果服务运行时间小于预热时间，则重新计算服务权重，即降权
                if (uptime > 0 && uptime < warmup) {
                    // 重新计算服务权重
                    weight = calculateWarmupWeight(uptime, warmup, weight);
                }
            }
        }
        return weight >= 0 ? weight : 0;
    }

}
