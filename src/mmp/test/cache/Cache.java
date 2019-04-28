package mmp.test.cache;


import mmp.container.DelayQueue;

import java.lang.Thread;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;


/*
应用场景
DelayedQueue可以快速找到要失效的对象，但DelayedQueue内部的PriorityQueue的插入、删除时的排序也耗费资源

a) 关闭空闲连接。服务器中，有很多客户端的连接，空闲一段时间之后需要关闭之。
b) 缓存。缓存中的对象，超过了空闲时间，需要从缓存中移出。
c) 任务超时处理。在网络协议滑动窗口请求应答式交互时，处理超时未响应的请求。
d) Session超时管理。网络应答通讯协议的请求超时处理。
* */
public class Cache<K, V> {


    private ConcurrentMap<K, V> cacheObjMap = new ConcurrentHashMap<>();

    private DelayQueue<DelayItem<Pair<K, V>>> q = new DelayQueue<>();

    private Thread daemonThread;

    public Cache() {

        Runnable daemonTask = this::daemonCheck;
        daemonThread = new Thread(daemonTask);
        daemonThread.setDaemon(true);
        daemonThread.setName("Cache Daemon");
        daemonThread.start();
    }

    // 当缓存关闭，监控程序也应关闭，因而监控线程应当用守护线程
    private void daemonCheck() {
        for (; ; ) {
            try {
                DelayItem<Pair<K, V>> delayItem = q.take();
                if (delayItem != null) {
                    // 超时对象处理
                    Pair<K, V> pair = delayItem.getItem();
                    cacheObjMap.remove(pair.key, pair.value); // compare and remove
                }
            } catch (InterruptedException e) {
                break;
            }
        }


    }



    // 添加缓存对象
    public void put(K key, V value, long time, TimeUnit unit) {
        V oldValue = cacheObjMap.put(key, value);
        // 当向缓存中添加key-value对时，如果这个key在缓存中存在并且还没有过期，需要用这个key对应的新过期时间
        if (oldValue != null) {
            // 为了能够让DelayQueue将其已保存的key删除，需要重写实现Delayed接口添加到DelayQueue的DelayedItem的hashCode函数和equals函数
            boolean result = q.remove(new DelayItem<>(new Pair<>(key, oldValue), 0L));
            System.out.println("remove:=" + result);
        }

        long nanoTime = TimeUnit.NANOSECONDS.convert(time, unit);
        q.put(new DelayItem<>(new Pair<>(key, value), nanoTime));
    }

    public V get(K key) {
        return cacheObjMap.get(key);
    }





    public DelayQueue<DelayItem<Pair<K, V>>> getQ() {
        return q;
    }

    public void setQ(DelayQueue<DelayItem<Pair<K, V>>> q) {
        this.q = q;
    }

    // 测试入口函数
    public static void main(String[] args) throws Exception {
        Cache<Integer, String> cache = new Cache<>();
        cache.put(1, "aaaa", 60, TimeUnit.SECONDS);
        cache.put(1, "aaaa", 10, TimeUnit.SECONDS);
        //cache.put(1, "ccc", 60, TimeUnit.SECONDS);
        cache.put(2, "bbbb", 30, TimeUnit.SECONDS);
        cache.put(3, "cccc", 66, TimeUnit.SECONDS);
        cache.put(4, "dddd", 54, TimeUnit.SECONDS);
        cache.put(5, "eeee", 35, TimeUnit.SECONDS);
        cache.put(6, "ffff", 38, TimeUnit.SECONDS);
        cache.put(1, "aaaa", 70, TimeUnit.SECONDS);

        for (; ; ) {
            Thread.sleep(1000 * 2);
            for (Object obj : cache.getQ().toArray()) {
                System.out.print((obj).toString());
                System.out.println(",");
            }
            System.out.println();
        }
    }
}
