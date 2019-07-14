package org.apache.dubbo.rpc.cluster.loadbalance;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.support.RpcUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SPLIT_PATTERN;

/**
 * hash一致性
 * 首先根据ip或者其他的信息为缓存节点生成一个hash，并将这个hash投射到[0, 232 - 1]的圆环上
 * 当有查询或写入请求时，则为缓存项的key生成一个hash值
 * 然后查找第一个大于或等于该hash值的缓存节点，并到这个节点中查询或写入缓存项
 * 如果当前节点挂了，则在下一次查询或写入缓存时，为缓存项查找另一个大于其hash值的缓存节点即可
 * 如果缓存项的key的hash值小于缓存节点hash值，则到该缓存节点中存储或读取缓存项
 * 由于cache-3挂了，原本应该存到该节点中的缓存项最终会存储到cache-4节点中
 */
public class ConsistentHashLoadBalance extends AbstractLoadBalance {
    public static final String NAME = "consistenthash";

    public static final String HASH_NODES = "hash.nodes";

    public static final String HASH_ARGUMENTS = "hash.arguments";

    private final ConcurrentMap<String, ConsistentHashSelector<?>> selectors = new ConcurrentHashMap<String, ConsistentHashSelector<?>>();

    @SuppressWarnings("unchecked")
    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        String methodName = RpcUtils.getMethodName(invocation);
        String key = invokers.get(0).getUrl().getServiceKey() + "." + methodName;
        // 获取invokers原始的hashcode
        int identityHashCode = System.identityHashCode(invokers);
        ConsistentHashSelector<T> selector = (ConsistentHashSelector<T>) selectors.get(key);
        // 如果invokers是一个新的List对象，意味着服务提供者数量发生了变化，可能新增也可能减少了
        // 此时selector.identityHashCode != identityHashCode条件成立
        if (selector == null || selector.identityHashCode != identityHashCode) {
            // 创建新的ConsistentHashSelector
            selectors.put(key, new ConsistentHashSelector<T>(invokers, methodName, identityHashCode));
            selector = (ConsistentHashSelector<T>) selectors.get(key);
        }
        // 调用ConsistentHashSelector的select方法选择Invoker
        return selector.select(invocation);
    }

    private static final class ConsistentHashSelector<T> {

        /**
         * TreeMap存储Invoker虚拟节点
         */
        private final TreeMap<Long, Invoker<T>> virtualInvokers;

        private final int replicaNumber;

        private final int identityHashCode;

        private final int[] argumentIndex;

        ConsistentHashSelector(List<Invoker<T>> invokers, String methodName, int identityHashCode) {
            this.virtualInvokers = new TreeMap<Long, Invoker<T>>();
            this.identityHashCode = identityHashCode;
            URL url = invokers.get(0).getUrl();
            // 获取虚拟节点数，默认为160
            this.replicaNumber = url.getMethodParameter(methodName, HASH_NODES, 160);
            // 获取参与hash计算的参数下标值，默认对第一个参数进行hash运算
            String[] index = COMMA_SPLIT_PATTERN.split(url.getMethodParameter(methodName, HASH_ARGUMENTS, "0"));
            argumentIndex = new int[index.length];
            for (int i = 0; i < index.length; i++) {
                argumentIndex[i] = Integer.parseInt(index[i]);
            }
            for (Invoker<T> invoker : invokers) {
                String address = invoker.getUrl().getAddress();
                for (int i = 0; i < replicaNumber / 4; i++) {
                    // 对 address + i 进行md5运算，得到一个长度为16的字节数组
                    byte[] digest = md5(address + i);
                    // 对digest部分字节进行4次hash运算，得到四个不同的long型正整数
                    for (int h = 0; h < 4; h++) {
                        // h = 0 时，取 digest 中下标为 0 ~ 3 的4个字节进行位运算
                        // h = 1 时，取 digest 中下标为 4 ~ 7 的4个字节进行位运算
                        // h = 2, h = 3 时过程同上
                        long m = hash(digest, h);
                        // 将hash到invoker的映射关系存储到virtualInvokers中，virtualInvokers需要提供高效的查询操作，因此选用TreeMa
                        virtualInvokers.put(m, invoker);
                    }
                }
            }
        }

        public Invoker<T> select(Invocation invocation) {
            // 将参数转为key
            String key = toKey(invocation.getArguments());
            // 对参数key进行md5
            byte[] digest = md5(key);
            // 取digest数组的前四个字节进行hash运算，再将hash传给selectForKey方法，寻找合适的Invoker
            return selectForKey(hash(digest, 0));
        }

        private String toKey(Object[] args) {
            StringBuilder buf = new StringBuilder();
            for (int i : argumentIndex) {
                if (i >= 0 && i < args.length) {
                    buf.append(args[i]);
                }
            }
            return buf.toString();
        }

        private Invoker<T> selectForKey(long hash) {
            // 到TreeMap中查找第一个节点值大于或等于当前hash的Invoker
            Map.Entry<Long, Invoker<T>> entry = virtualInvokers.ceilingEntry(hash);
            // 如果hash大于Invoker在圆环上最大的位置，此时entry = null，需要将TreeMap的头节点赋值给entry
            if (entry == null) {
                entry = virtualInvokers.firstEntry();
            }
            // 返回Invoker
            return entry.getValue();
        }

        private long hash(byte[] digest, int number) {
            return (((long) (digest[3 + number * 4] & 0xFF) << 24) | ((long) (digest[2 + number * 4] & 0xFF) << 16) | ((long) (digest[1 + number * 4] & 0xFF) << 8) | (digest[number * 4] & 0xFF)) & 0xFFFFFFFFL;
        }

        private byte[] md5(String value) {
            MessageDigest md5;
            try {
                md5 = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            md5.reset();
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            md5.update(bytes);
            return md5.digest();
        }

    }

}
