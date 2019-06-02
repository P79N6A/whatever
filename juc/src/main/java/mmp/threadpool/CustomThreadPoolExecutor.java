package mmp.threadpool;

import mmp.container.LinkedBlockingQueue;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class CustomThreadPoolExecutor {

    /**
     * corePoolSize 线程池中核心线程数量
     * maximumPoolSize 最大线程数量
     * keepAliveTime 空闲时间（线程池的线程数超过核心数量时，空闲线程的存活时间
     * unit 时间单位
     * workQueue 核心线程满时，存储任务的队列
     * threadFactory 创建线程的工厂
     * handler 当队列满了之后的拒绝策略
     */
    private static final mmp.threadpool.ExecutorService threadPoolExecutor = new mmp.threadpool.ThreadPoolExecutor(5, 20, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(1024), new mmp.threadpool.ThreadPoolExecutor.AbortPolicy());

    /**
     * 返回一个固定线程数量的线程池
     * 新任务提交时，线程池中若有空闲线程，立即执行，若没有，则暂存在一个任务队列中，待有线程空闲时处理
     */
    private static final mmp.threadpool.ExecutorService fixedThreadPool = mmp.threadpool.Executors.newFixedThreadPool(5);

    /**
     * 返回一个只有一个线程的线程池
     * 若多一个任务被提交到该线程池，任务被保存在一个任务队列中，待线程空闲，按FIFO顺序执行
     */
    private static final mmp.threadpool.ExecutorService singleThreadExecutor = mmp.threadpool.Executors.newSingleThreadExecutor();

    /**
     * 返回一个可根据实际情况调整线程数量的线程池
     * 线程池的线程数量不确定，但若有空闲线程可以复用，则会优先使用可复用的线程，所有线程均在工作，如果有新的任务提交，则会创建新的线程处理任务
     * 所有线程在当前任务执行完毕后，将返回线程池进行复用
     */
    private static final mmp.threadpool.ExecutorService cachedThreadPool = mmp.threadpool.Executors.newCachedThreadPool();

    private static final mmp.threadpool.ExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(2);

    public static void main(String[] args) {

        // 在给定delay后执行
        ((mmp.threadpool.ScheduledExecutorService) scheduledThreadPool).schedule(() -> {
        }, 5, TimeUnit.SECONDS);

        // initialDelay（初始延迟）表示第一次延时时间
        // period表示间隔时间
        // 将在initialDelay后开始执行，然后在initialDelay+period后执行，接着在initialDelay + 2 * period后执行，依此类推
        // 如果前面的任务没有完成，则调度也不会启动
        ((mmp.threadpool.ScheduledExecutorService) scheduledThreadPool).scheduleAtFixedRate(() -> {
        }, 0, 2, TimeUnit.SECONDS);

        // initialDelay（初始延迟）表示延时时间
        // delay + 任务执行时间 = 间隔时间period
        // 每一次执行终止和下一次执行开始之间都存在给定的延迟
        ((mmp.threadpool.ScheduledExecutorService) scheduledThreadPool).scheduleWithFixedDelay(() -> {
        }, 0, 2, TimeUnit.SECONDS);



        /*
         * submit有返回值，execute没有
         * submit返回值可以执行cancel取消操作，或者调用get
         */

        Future<String> future = threadPoolExecutor.submit(() -> "MMP");

        threadPoolExecutor.execute(() -> {
        });

    }

}
