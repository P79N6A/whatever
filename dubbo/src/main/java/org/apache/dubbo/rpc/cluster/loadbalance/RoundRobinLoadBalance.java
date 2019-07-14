package org.apache.dubbo.rpc.cluster.loadbalance;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 平滑加权轮询
 * 从最简单的轮询开始讲起，所谓轮询是指将请求轮流分配给每台服务器
 * 有三台服务器A、B、C
 * 将第一个请求分配给服务器A，第二个请求分配给服务器B，第三个请求分配给服务器C，第四个请求再次分配给服务器A，这个过程就叫做轮询
 * 轮询是一种无状态负载均衡算法，实现简单，适用于每台服务器性能相近的场景下
 * 对轮询过程进行加权，以调控每台服务器的负载，经过加权后，每台服务器能够得到的请求数比例，接近或等于他们的权重比
 * 比如服务器A、B、C权重比为 5:2:1
 * 那么在8次请求中，服务器A将收到其中的5次请求，服务器B会收到其中的2次请求，服务器C则收到其中的1次请求
 */
public class RoundRobinLoadBalance extends AbstractLoadBalance {
    public static final String NAME = "roundrobin";

    private static final int RECYCLE_PERIOD = 60000;

    protected static class WeightedRoundRobin {
        /**
         * 服务提供者权重
         */
        private int weight;

        /**
         * 当前权重
         */
        private AtomicLong current = new AtomicLong(0);

        /**
         * 最后一次更新时间
         */
        private long lastUpdate;

        public int getWeight() {
            return weight;
        }

        public void setWeight(int weight) {
            this.weight = weight;
            // 初始情况下，current = 0
            current.set(0);
        }

        public long increaseCurrent() {
            // current = current + weight；
            return current.addAndGet(weight);
        }

        public void sel(int total) {
            // current = current - total;
            current.addAndGet(-1 * total);
        }

        public long getLastUpdate() {
            return lastUpdate;
        }

        public void setLastUpdate(long lastUpdate) {
            this.lastUpdate = lastUpdate;
        }

    }

    // 嵌套 Map 结构，存储的数据结构示例如下：
    // {
    //     "UserService.query": {
    //         "url1": WeightedRoundRobin@123,
    //         "url2": WeightedRoundRobin@456,
    //     },
    //     "UserService.update": {
    //         "url1": WeightedRoundRobin@123,
    //         "url2": WeightedRoundRobin@456,
    //     }
    // }
    // 最外层为服务类名 + 方法名，第二层为url到WeightedRoundRobin的映射关系，这里可以将url看成是服务提供者的id
    private ConcurrentMap<String, ConcurrentMap<String, WeightedRoundRobin>> methodWeightMap = new ConcurrentHashMap<String, ConcurrentMap<String, WeightedRoundRobin>>();

    /**
     * 原子更新锁
     */
    private AtomicBoolean updateLock = new AtomicBoolean();

    protected <T> Collection<String> getInvokerAddrList(List<Invoker<T>> invokers, Invocation invocation) {
        String key = invokers.get(0).getUrl().getServiceKey() + "." + invocation.getMethodName();
        Map<String, WeightedRoundRobin> map = methodWeightMap.get(key);
        if (map != null) {
            return map.keySet();
        }
        return null;
    }

    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        // key = 全限定类名 + "." + 方法名，比如 com.xxx.DemoService.sayHello
        String key = invokers.get(0).getUrl().getServiceKey() + "." + invocation.getMethodName();
        // 获取url到WeightedRoundRobin映射表，如果为空，则创建一个新的
        ConcurrentMap<String, WeightedRoundRobin> map = methodWeightMap.get(key);
        if (map == null) {
            methodWeightMap.putIfAbsent(key, new ConcurrentHashMap<String, WeightedRoundRobin>());
            map = methodWeightMap.get(key);
        }
        int totalWeight = 0;
        long maxCurrent = Long.MIN_VALUE;
        // 获取当前时间
        long now = System.currentTimeMillis();
        Invoker<T> selectedInvoker = null;
        WeightedRoundRobin selectedWRR = null;
        // 下面这个循环主要做了这样几件事情：
        //   1. 遍历Invoker列表，检测当前Invoker是否有相应的WeightedRoundRobin，没有则创建
        //   2. 检测Invoker权重是否发生了变化，若变化了，则更新WeightedRoundRobin的weight 字段
        //   3. 让current字段加上自身权重，等价于current += weight
        //   4. 设置lastUpdate 字段，即lastUpdate = now
        //   5. 寻找具有最大current的Invoker，以及Invoker对应的WeightedRoundRobin，暂存起来，留作后用
        //   6. 计算权重总和
        for (Invoker<T> invoker : invokers) {
            String identifyString = invoker.getUrl().toIdentityString();
            WeightedRoundRobin weightedRoundRobin = map.get(identifyString);
            int weight = getWeight(invoker, invocation);
            // 检测当前Invoker是否有对应的WeightedRoundRobin，没有则创建
            if (weightedRoundRobin == null) {
                weightedRoundRobin = new WeightedRoundRobin();
                // 设置Invoker权重
                weightedRoundRobin.setWeight(weight);
                // 存储url唯一标识identifyString到weightedRoundRobin的映射关系
                map.putIfAbsent(identifyString, weightedRoundRobin);
            }
            // Invoker权重不等于WeightedRoundRobin中保存的权重，说明权重变化了，此时进行更新
            if (weight != weightedRoundRobin.getWeight()) {
                weightedRoundRobin.setWeight(weight);
            }
            // 让current加上自身权重，等价于current += weight
            long cur = weightedRoundRobin.increaseCurrent();
            // 设置lastUpdate，表示近期更新过
            weightedRoundRobin.setLastUpdate(now);
            // 找出最大的current
            if (cur > maxCurrent) {
                maxCurrent = cur;
                // 将具有最大current权重的Invoker赋值给selectedInvoker
                selectedInvoker = invoker;
                // 将Invoker对应的weightedRoundRobin赋值给selectedWRR，留作后用
                selectedWRR = weightedRoundRobin;
            }
            // 计算权重总和
            totalWeight += weight;
        }
        // 对 <identifyString, WeightedRoundRobin> 进行检查，过滤掉长时间未被更新的节点
        // 该节点可能挂了，invokers中不包含该节点，所以该节点的lastUpdate长时间无法被更新
        // 若未更新时长超过阈值后，就会被移除掉，默认阈值为60秒
        if (!updateLock.get() && invokers.size() != map.size()) {
            if (updateLock.compareAndSet(false, true)) {
                try {
                    ConcurrentMap<String, WeightedRoundRobin> newMap = new ConcurrentHashMap<String, WeightedRoundRobin>();
                    // 拷贝
                    newMap.putAll(map);
                    Iterator<Entry<String, WeightedRoundRobin>> it = newMap.entrySet().iterator();
                    // 遍历修改，即移除过期记录
                    while (it.hasNext()) {
                        Entry<String, WeightedRoundRobin> item = it.next();
                        if (now - item.getValue().getLastUpdate() > RECYCLE_PERIOD) {
                            it.remove();
                        }
                    }
                    // 更新引用
                    methodWeightMap.put(key, newMap);
                } finally {
                    updateLock.set(false);
                }
            }
        }
        if (selectedInvoker != null) {
            // 让current减去权重总和，等价于current -= totalWeight
            selectedWRR.sel(totalWeight);
            // 返回具有最大current的Invoker
            return selectedInvoker;
        }
        // should not happen here
        return invokers.get(0);
    }

}
