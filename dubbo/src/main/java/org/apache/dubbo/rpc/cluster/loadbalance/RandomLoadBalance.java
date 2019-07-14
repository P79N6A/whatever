package org.apache.dubbo.rpc.cluster.loadbalance;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 权重随机
 * 假设有一组服务器servers = [A, B, C]，对应的权重为weights = [5, 3, 2]，权重总和为10
 * 现在把这些权重值平铺在一维坐标值上，[0, 5)区间属于服务器A，[5, 8)区间属于服务器B，[8, 10)区间属于服务器C
 * 接下来通过随机数生成器生成一个范围在[0, 10)之间的随机数，然后计算这个随机数会落到哪个区间上
 * 比如数字3会落到服务器A对应的区间上，此时返回服务器A即可
 * 权重越大的机器，在坐标轴上对应的区间范围就越大，因此随机数生成器生成的数字就会有更大的概率落到此区间内
 * 只要随机数生成器产生的随机数分布性很好，在经过多次选择后，每个服务器被选中的次数比例接近其权重比例
 * 比如，经过一万次选择后，服务器A被选中的次数大约为5000次，服务器B被选中的次数约为3000次，服务器C被选中的次数约为2000次
 */
public class RandomLoadBalance extends AbstractLoadBalance {

    public static final String NAME = "random";

    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        int length = invokers.size();
        boolean sameWeight = true;
        int[] weights = new int[length];
        int firstWeight = getWeight(invokers.get(0), invocation);
        weights[0] = firstWeight;
        int totalWeight = firstWeight;
        // 下面这个循环有两个作用，一是计算总权重totalWeight，二是检测每个服务提供者的权重是否相同
        for (int i = 1; i < length; i++) {
            int weight = getWeight(invokers.get(i), invocation);
            weights[i] = weight;
            // 累加权重
            totalWeight += weight;
            // 检测当前服务提供者的权重与上一个服务提供者的权重是否相同，不相同的话，则将sameWeight置为false
            if (sameWeight && weight != firstWeight) {
                sameWeight = false;
            }
        }
        // 下面的if分支主要用于获取随机数，并计算随机数落在哪个区间上
        if (totalWeight > 0 && !sameWeight) {
            // 随机获取一个[0, totalWeight)区间内的数字
            int offset = ThreadLocalRandom.current().nextInt(totalWeight);
            // 循环让offset数减去服务提供者权重值，offset 小于0时，返回相应的Invoker
            // 有servers = [A, B, C]，weights = [5, 3, 2]，offset = 7
            // 第一次循环，offset - 5 = 2 > 0，offset > 5，表明其不会落在服务器A对应的区间上
            // 第二次循环，offset - 3 = -1 < 0，5 < offset < 8，表明其会落在服务器B对应的区间上
            for (int i = 0; i < length; i++) {
                // 让随机值offset减去权重值
                offset -= weights[i];
                if (offset < 0) {
                    // 返回相应的Invoker
                    return invokers.get(i);
                }
            }
        }
        // 如果所有服务提供者权重值相同，此时直接随机返回一个即可
        return invokers.get(ThreadLocalRandom.current().nextInt(length));
    }

}
