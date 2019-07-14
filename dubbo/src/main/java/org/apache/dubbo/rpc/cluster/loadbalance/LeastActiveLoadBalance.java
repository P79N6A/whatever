package org.apache.dubbo.rpc.cluster.loadbalance;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcStatus;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 最少活跃调用数
 * 活跃调用数越小，表明该服务提供者效率越高，单位时间内可处理更多的请求
 * 此时应优先将请求分配给该服务提供者
 * 在具体实现中，每个服务提供者对应一个活跃数active
 * 初始情况下，所有服务提供者活跃数均为0
 * 每收到一个请求，活跃数加1，完成请求后则将活跃数减1
 * 在服务运行一段时间后，性能好的服务提供者处理请求的速度更快，因此活跃数下降的也越快，此时这样的服务提供者能够优先获取到新的服务请求
 * 除了最小活跃数，LeastActiveLoadBalance在实现上还引入了权重值
 * 所以准确的来说，LeastActiveLoadBalance是基于加权最小活跃数算法实现的
 * 举个例子，在一个服务提供者集群中，有两个性能优异的服务提供者
 * 某一时刻它们的活跃数相同，此时根据它们的权重去分配请求，权重越大，获取到新请求的概率就越大
 * 如果两个服务提供者权重相同，此时随机选择一个即可
 */
public class LeastActiveLoadBalance extends AbstractLoadBalance {

    public static final String NAME = "leastactive";

    /**
     * 遍历invokers列表，寻找活跃数最小的Invoker
     * 如果有多个Invoker具有相同的最小活跃数，此时记录下这些Invoker在invokers集合中的下标，并累加它们的权重，比较它们的权重值是否相等
     * 如果只有一个Invoker具有最小的活跃数，此时直接返回该Invoker即可
     * 如果有多个Invoker具有最小活跃数，且它们的权重不相等，此时处理方式和RandomLoadBalance一致
     * 如果有多个Invoker具有最小活跃数，但它们的权重相等，此时随机返回一个即可
     */
    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        int length = invokers.size();
        // 最小的活跃数
        int leastActive = -1;
        // 具有相同“最小活跃数”的服务者提供者（以下用 Invoker 代称）数量
        int leastCount = 0;
        // leastIndexes用于记录具有相同“最小活跃数”的Invoker在invokers列表中的下标信息
        int[] leastIndexes = new int[length];
        int[] weights = new int[length];
        // 第一个最小活跃数的Invoker权重值，用于与其他具有相同最小活跃数的Invoker的权重进行对比，以检测是否“所有具有相同最小活跃数的Invoker的权重”均相等
        int totalWeight = 0;
        int firstWeight = 0;
        boolean sameWeight = true;
        // 遍历invokers列表
        for (int i = 0; i < length; i++) {
            Invoker<T> invoker = invokers.get(i);
            // 获取Invoker对应的活跃数
            int active = RpcStatus.getStatus(invoker.getUrl(), invocation.getMethodName()).getActive();
            int afterWarmup = getWeight(invoker, invocation);
            // 获取权重 - ⭐️
            weights[i] = afterWarmup;
            // 发现更小的活跃数，重新开始
            if (leastActive == -1 || active < leastActive) {
                // 使用当前活跃数active更新最小活跃数leastActive
                leastActive = active;
                // 更新leastCount为1
                leastCount = 1;
                // 记录当前下标值到leastIndexes
                leastIndexes[0] = i;
                totalWeight = afterWarmup;
                firstWeight = afterWarmup;
                sameWeight = true;

            }
            // 当前Invoker的活跃数active与最小活跃数leastActive相同
            else if (active == leastActive) {
                // 在leastIndexes中记录下当前Invoker在invokers集合中的下标
                leastIndexes[leastCount++] = i;
                // 累加权重
                totalWeight += afterWarmup;
                // 检测当前Invoker的权重与firstWeight是否相等，不相等则将sameWeight置为false
                if (sameWeight && i > 0 && afterWarmup != firstWeight) {
                    sameWeight = false;
                }
            }
        }
        // 当只有一个Invoker具有最小活跃数，此时直接返回该Invoker即可
        if (leastCount == 1) {
            return invokers.get(leastIndexes[0]);
        }
        // 有多个Invoker具有相同的最小活跃数，但它们之间的权重不同
        if (!sameWeight && totalWeight > 0) {
            // 随机生成一个[0, totalWeight) 之间的数字
            int offsetWeight = ThreadLocalRandom.current().nextInt(totalWeight);
            // 循环让随机数减去具有最小活跃数的Invoker的权重值，offset小于等于0时，返回相应的Invoker
            for (int i = 0; i < leastCount; i++) {
                int leastIndex = leastIndexes[i];
                // 获取权重值，并让随机数减去权重值 - ⭐️
                offsetWeight -= weights[leastIndex];
                if (offsetWeight < 0) {
                    return invokers.get(leastIndex);
                }
            }
        }
        // 如果权重相同或权重为0时，随机返回一个Invoker
        return invokers.get(leastIndexes[ThreadLocalRandom.current().nextInt(leastCount)]);
    }

}