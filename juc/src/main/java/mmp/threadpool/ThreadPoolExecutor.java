package mmp.threadpool;


import java.lang.Thread;

import mmp.atomic.AtomicInteger;
import mmp.container.BlockingQueue;
import mmp.lock.AbstractQueuedSynchronizer;
import mmp.lock.Condition;
import mmp.lock.ReentrantLock;

import java.security.*;
import java.util.*;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class ThreadPoolExecutor extends AbstractExecutorService {

    // 记录线程池中的任务数量和线程池状态 高低位
    // workerCount：当前有效的线程数
    // runState：线程池的五种状态，Running、Shutdown、Stop、Tidying、Terminate
    // int一共有32位，runState至少需要3位，故workCount有29位，所以线程池的有效线程数最多为...
    // 初始化为RUNNING状态，并且任务数量初始化为0
    private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));


    private static final int COUNT_BITS = Integer.SIZE - 3; // 32-3=29，线程数量所占位数

    // 00000000 00000000 00000001 --> 001 0000 00000000 00000000 00000000
    // 000 11111 11111111 11111111 11111111
    private static final int CAPACITY = (1 << COUNT_BITS) - 1; // 低29位表示最大线程数


    // int型变量高3位（含符号位）
    private static final int RUNNING = -1 << COUNT_BITS; // 初始化状态，能够接收新任务，处理已添加的任务
    private static final int SHUTDOWN = 0 << COUNT_BITS; // shutdown，不接收新任务，但能处理已添加的任务
    private static final int STOP = 1 << COUNT_BITS; // shutdownNow，不接收新任务，不处理已添加的任务，中断正在处理的任务

    // 变为TIDYING状态时，会执行钩子函数terminated()
    private static final int TIDYING = 2 << COUNT_BITS; // 所有的任务已终止，ctl的任务数量为0，线程池会变为TIDYING状态
    private static final int TERMINATED = 3 << COUNT_BITS; // 线程池彻底终止 TIDYING状态时，执行完terminated之后

    // RUNNING-->SHUTDOWN-->STOP-->TIDYING-->TERMINATED
    // ~ 按位取反
    private static int runStateOf(int c) {
        return c & ~CAPACITY;
    }


    // & 000 11111 11111111 11111111 11111111 舍弃前三位
    private static int workerCountOf(int c) {
        return c & CAPACITY;
    }

    private static int ctlOf(int rs, int wc) {
        return rs | wc;
    }

    private static boolean runStateLessThan(int c, int s) {
        return c < s;
    }

    private static boolean runStateAtLeast(int c, int s) {
        return c >= s;
    }

    private static boolean isRunning(int c) {
        return c < SHUTDOWN;
    }

    private boolean compareAndIncrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect + 1);
    }

    private boolean compareAndDecrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect - 1);
    }

    private void decrementWorkerCount() {
        do {
        } while (!compareAndDecrementWorkerCount(ctl.get()));
    }

    // 阻塞队列
    private final BlockingQueue<Runnable> workQueue;

    // 互斥锁
    private final ReentrantLock mainLock = new ReentrantLock();

    // 线程集合，一个Worker对应一个线程
    private final HashSet<Worker> workers = new HashSet<Worker>();

    // 终止条件
    private final Condition termination = mainLock.newCondition();

    // 线程池中线程数量曾经达到过的最大值
    private int largestPoolSize;

    // ThreadFactory对象，用于创建线程
    private volatile ThreadFactory threadFactory;

    // 拒绝策略
    private volatile RejectedExecutionHandler handler;

    // 线程存活时间
    private volatile long keepAliveTime;

    private volatile boolean allowCoreThreadTimeOut;

    // 已完成任务数量
    private long completedTaskCount;

    // 核心池大小
    private volatile int corePoolSize;

    // 最大池大小
    private volatile int maximumPoolSize;

    // 默认拒绝策略
    private static final RejectedExecutionHandler defaultHandler = new AbortPolicy();

    private static final RuntimePermission shutdownPerm = new RuntimePermission("modifyThread");

    private final AccessControlContext acc;

    // 继承了AQS，实现了Runnable
    private final class Worker extends AbstractQueuedSynchronizer implements Runnable {


        final Thread thread;

        Runnable firstTask;

        volatile long completedTasks;

        Worker(Runnable firstTask) {
            // 设置AQS的同步状态为-1，防止执行shutDown，禁止中断，直到调用runWorker，设为0
            // shutDownNow同样也判断了state必须大于等于0才能interrupt
            setState(-1); // inhibit interrupts until runWorker
            this.firstTask = firstTask;
            // 通过线程工厂来创建线程，将自身传递
            this.thread = getThreadFactory().newThread(this);
        }

        public void run() {
            runWorker(this);
        }

        // Lock methods
        //
        // The value 0 represents the unlocked state.
        // The value 1 represents the locked state.

        protected boolean isHeldExclusively() {
            return getState() != 0;
        }

        protected boolean tryAcquire(int unused) {
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        protected boolean tryRelease(int unused) {
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }

        public void lock() {
            acquire(1);
        }

        public boolean tryLock() {
            return tryAcquire(1);
        }

        public void unlock() {
            release(1);
        }

        void interruptIfStarted() {
            Thread t;
            if (getState() >= 0 && (t = thread) != null && !t.isInterrupted()) {
                try {
                    t.interrupt();
                } catch (SecurityException ignore) {
                }
            }
        }
    }


    private void advanceRunState(int targetState) {
        for (; ; ) {
            int c = ctl.get();
            if (runStateAtLeast(c, targetState) || ctl.compareAndSet(c, ctlOf(targetState, workerCountOf(c)))) break;
        }
    }

    final void tryTerminate() {
        for (; ; ) {
            int c = ctl.get();
            if (isRunning(c) || runStateAtLeast(c, TIDYING) || (runStateOf(c) == SHUTDOWN && !workQueue.isEmpty()))
                return;
            if (workerCountOf(c) != 0) {
                interruptIdleWorkers(ONLY_ONE);
                return;
            }

            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                if (ctl.compareAndSet(c, ctlOf(TIDYING, 0))) {
                    try {
                        terminated();
                    } finally {
                        ctl.set(ctlOf(TERMINATED, 0));
                        termination.signalAll();
                    }
                    return;
                }
            } finally {
                mainLock.unlock();
            }
            // else retry on failed CAS
        }
    }

    private void checkShutdownAccess() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(shutdownPerm);
            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                for (ThreadPoolExecutor.Worker w : workers)
                    security.checkAccess(w.thread);
            } finally {
                mainLock.unlock();
            }
        }
    }

    private void interruptWorkers() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (ThreadPoolExecutor.Worker w : workers)
                w.interruptIfStarted(); // 中断全部线程，不管是否在执行
        } finally {
            mainLock.unlock();
        }
    }

    private void interruptIdleWorkers(boolean onlyOne) {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            // 遍历所有线程
            for (ThreadPoolExecutor.Worker w : workers) {
                Thread t = w.thread;
                // 多了一个条件w.tryLock()，表示拿到锁后就中断
                if (!t.isInterrupted() && w.tryLock()) {
                    try {
                        t.interrupt();
                    } catch (SecurityException ignore) {
                    } finally {
                        w.unlock();
                    }
                }
                if (onlyOne) break;
            }
        } finally {
            mainLock.unlock();
        }
    }

    private void interruptIdleWorkers() {
        interruptIdleWorkers(false);
    }

    private static final boolean ONLY_ONE = true;


    final void reject(Runnable command) {
        handler.rejectedExecution(command, this);
    }

    void onShutdown() {
    }

    final boolean isRunningOrShutdown(boolean shutdownOK) {
        int rs = runStateOf(ctl.get());
        return rs == RUNNING || (rs == SHUTDOWN && shutdownOK);
    }

    private List<Runnable> drainQueue() {
        BlockingQueue<Runnable> q = workQueue;
        ArrayList<Runnable> taskList = new ArrayList<Runnable>();
        q.drainTo(taskList);
        if (!q.isEmpty()) {
            for (Runnable r : q.toArray(new Runnable[0])) {
                if (q.remove(r)) taskList.add(r);
            }
        }
        return taskList;
    }

    // 创建线程
    private boolean addWorker(Runnable firstTask, boolean core) {
        retry:
        for (; ; ) {
            int c = ctl.get();
            int rs = runStateOf(c);
            // 再次检查线程池是否处于运行状态
            if (rs >= SHUTDOWN && !(rs == SHUTDOWN && firstTask == null && !workQueue.isEmpty())) return false;

            for (; ; ) {
                // 线程池中线程的数量
                int wc = workerCountOf(c);
                // 判断线程数量是否溢出
                if (wc >= CAPACITY || wc >= (core ? corePoolSize : maximumPoolSize)) return false;
                // CAS将正在运行的线程数+1，成功则退出循环
                if (compareAndIncrementWorkerCount(c)) break retry;
                c = ctl.get();  // Re-read ctl
                // 检查线程池运行状态，如果与之前的状态不同，则从retry重新开始
                if (runStateOf(c) != rs) continue retry;
                // else CAS failed due to workerCount change; retry inner loop
            }
        }

        // 正在运行的线程数自增成功后则将线程封装成工作线程Worker
        boolean workerStarted = false;
        boolean workerAdded = false;
        ThreadPoolExecutor.Worker w = null;
        // 添加任务到线程池，并启动任务所在的线程
        try {
            // 新建Worker，并且指定firstTask为Worker的第一个任务
            w = new ThreadPoolExecutor.Worker(firstTask);
            // 获取Worker对应的线程
            final Thread t = w.thread;
            if (t != null) {
                // 获取锁
                final ReentrantLock mainLock = this.mainLock;
                mainLock.lock();
                try {
                    int rs = runStateOf(ctl.get());
                    // 再次确认线程池运行状态
                    if (rs < SHUTDOWN || (rs == SHUTDOWN && firstTask == null)) {
                        if (t.isAlive()) throw new IllegalThreadStateException();
                        // 将Worker对象添加到线程池的Worker集合中
                        workers.add(w);
                        // 更新largestPoolSize
                        int s = workers.size();
                        if (s > largestPoolSize) largestPoolSize = s;
                        workerAdded = true;
                    }
                } finally {
                    // 释放锁
                    mainLock.unlock();
                }
                // 如果成功将任务添加到线程池中，则启动任务所在的线程
                if (workerAdded) {
                    t.start();
                    workerStarted = true;
                }
            }
        } finally {
            // 在启动工作线程失败后，将工作线程从集合中移除
            if (!workerStarted) addWorkerFailed(w);
        }
        // 返回任务是否启动
        return workerStarted;
    }

    private void addWorkerFailed(ThreadPoolExecutor.Worker w) {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            if (w != null) workers.remove(w);
            decrementWorkerCount();
            tryTerminate();
        } finally {
            mainLock.unlock();
        }
    }

    private void processWorkerExit(ThreadPoolExecutor.Worker w, boolean completedAbruptly) {
        if (completedAbruptly) decrementWorkerCount();

        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            completedTaskCount += w.completedTasks;
            workers.remove(w);
        } finally {
            mainLock.unlock();
        }

        tryTerminate();

        int c = ctl.get();
        if (runStateLessThan(c, STOP)) {
            if (!completedAbruptly) {
                int min = allowCoreThreadTimeOut ? 0 : corePoolSize;
                if (min == 0 && !workQueue.isEmpty()) min = 1;
                if (workerCountOf(c) >= min) return;
            }
            addWorker(null, false);
        }
    }

    private Runnable getTask() {
        boolean timedOut = false; // Did the last poll() time out?

        for (; ; ) {
            int c = ctl.get();
            int rs = runStateOf(c);

            // Check if queue empty only if necessary.
            if (rs >= SHUTDOWN && (rs >= STOP || workQueue.isEmpty())) {
                decrementWorkerCount();
                return null;
            }

            int wc = workerCountOf(c);

            // Are workers subject to culling?
            boolean timed = allowCoreThreadTimeOut || wc > corePoolSize;

            if ((wc > maximumPoolSize || (timed && timedOut)) && (wc > 1 || workQueue.isEmpty())) {
                if (compareAndDecrementWorkerCount(c)) return null;
                continue;
            }

            try {
                Runnable r = timed ? workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) : workQueue.take();
                if (r != null) return r;
                timedOut = true;
            } catch (InterruptedException retry) {
                timedOut = false;
            }
        }
    }

    // 核心
    final void runWorker(ThreadPoolExecutor.Worker w) {
        Thread wt = Thread.currentThread();
        Runnable task = w.firstTask;
        w.firstTask = null;
        w.unlock(); // allow interrupts
        boolean completedAbruptly = true;
        try {
            // 不断从队列里边拿任务执行
            while (task != null || (task = getTask()) != null) {
                w.lock();
                // If pool is stopping, ensure thread is interrupted;
                // if not, ensure thread is not interrupted.
                // This requires a recheck in second case to deal with shutdownNow race while clearing interrupt
                if ((runStateAtLeast(ctl.get(), STOP) || (Thread.interrupted() && runStateAtLeast(ctl.get(), STOP))) && !wt.isInterrupted())
                    wt.interrupt();
                try {
                    // 切面
                    beforeExecute(wt, task);
                    Throwable thrown = null;
                    try {
                        // 首先执行firstTask的run方法，然后循环获取阻塞队列中的任务，并run
                        task.run();
                    } catch (RuntimeException x) {
                        thrown = x;
                        throw x;
                    } catch (Error x) {
                        thrown = x;
                        throw x;
                    } catch (Throwable x) {
                        thrown = x;
                        throw new Error(x);
                    } finally {
                        afterExecute(task, thrown);
                    }
                } finally {
                    task = null;
                    w.completedTasks++;
                    w.unlock();
                }
            }
            completedAbruptly = false;
        } finally {
            // 如果线程池中的任务异常，就抛出异常并停止运行线程池
            processWorkerExit(w, completedAbruptly);
        }
    }

    public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, Executors.defaultThreadFactory(), defaultHandler);
    }

    public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, defaultHandler);
    }

    public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, Executors.defaultThreadFactory(), handler);
    }

    public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        if (corePoolSize < 0 || maximumPoolSize <= 0 || maximumPoolSize < corePoolSize || keepAliveTime < 0)
            throw new IllegalArgumentException();
        if (workQueue == null || threadFactory == null || handler == null) throw new NullPointerException();
        this.acc = System.getSecurityManager() == null ? null : AccessController.getContext();
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.workQueue = workQueue;
        this.keepAliveTime = unit.toNanos(keepAliveTime);
        this.threadFactory = threadFactory;
        this.handler = handler;
    }

    // 执行
    public void execute(Runnable command) {
        if (command == null) throw new NullPointerException();
        int c = ctl.get();
        // 1 线程数量小于核心线程数，则创建线程
        if (workerCountOf(c) < corePoolSize) {
            // 添加成功则直接返回
            if (addWorker(command, true)) return;
            // 否则再次获取活动线程数量
            c = ctl.get();
        }
        // 2 核心线程数已满
        // 线程池正在运行 && 任务队列未满，添加进队列成功
        if (isRunning(c) && workQueue.offer(command)) {
            // 再次检查线程池状态， 因为上面addWorker过了并且失败了
            int recheck = ctl.get();
            // 如果状态不是运行状态，且从队列删除该任务成功并尝试停止线程池，拒绝任务
            if (!isRunning(recheck) && remove(command)) reject(command);
                // 如果当前工作线程数量为0（线程池已关闭），则添加一个空任务到队列中
            else if (workerCountOf(recheck) == 0) addWorker(null, false);
        }
        // 3 核心池已满，队列已满，添加队列失败，则创建一个任务线程，如果失败，则拒绝
        else if (!addWorker(command, false)) reject(command);

        // 1 和 3 新建线程时需要获取全局锁
    }

    // 不接受新任务，只结束未执行的任务
    public void shutdown() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            checkShutdownAccess(); // 获取权限
            advanceRunState(SHUTDOWN); // 修改运行状态
            interruptIdleWorkers(); // 遍历停止未开启的线程
            onShutdown(); // hook for ScheduledThreadPoolExecutor
        } finally {
            mainLock.unlock();
        }
        tryTerminate();
    }


    // shutdownNow() 结束全部，返回等待执行的任务列表
    // 遍历线程池中的线程，逐个Thread.interrupt() 来中断线程，所以无法响应中断的任务可能无法停止
    public List<Runnable> shutdownNow() {
        List<Runnable> tasks;
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            checkShutdownAccess();
            advanceRunState(STOP);
            interruptWorkers();
            tasks = drainQueue();
        } finally {
            mainLock.unlock();
        }
        tryTerminate();
        return tasks;
    }

    public boolean isShutdown() {
        return !isRunning(ctl.get());
    }


    public boolean isTerminated() {
        return runStateAtLeast(ctl.get(), TERMINATED);
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (; ; ) {
                if (runStateAtLeast(ctl.get(), TERMINATED)) return true;
                if (nanos <= 0) return false;
                nanos = termination.awaitNanos(nanos);
            }
        } finally {
            mainLock.unlock();
        }
    }

    protected void finalize() {
        SecurityManager sm = System.getSecurityManager();
        if (sm == null || acc == null) {
            shutdown();
        } else {
            PrivilegedAction<Void> pa = () -> {
                shutdown();
                return null;
            };
            AccessController.doPrivileged(pa, acc);
        }
    }


    public ThreadFactory getThreadFactory() {
        return threadFactory;
    }


    void ensurePrestart() {
        int wc = workerCountOf(ctl.get());
        if (wc < corePoolSize) addWorker(null, true);
        else if (wc == 0) addWorker(null, false);
    }


    public BlockingQueue<Runnable> getQueue() {
        return workQueue;
    }

    // 从队列中移除此任务
    public boolean remove(Runnable task) {
        boolean removed = workQueue.remove(task);
        tryTerminate(); // In case SHUTDOWN and now empty
        return removed;
    }


    // 子类重写beforeExecute, afterExecute, terminated方法

    // 执行任务之前
    protected void beforeExecute(Thread t, Runnable r) {
    }

    // 任务执行完毕后
    protected void afterExecute(Runnable r, Throwable t) {
    }

    // 线程池退出时
    protected void terminated() {
    }



    /*
     * AbortPolicy ：直接抛出异常
     * CallerRunsPolicy : 只要线程池未关闭，直接在调用者线程中，运行当前被丢弃的任务
     * DiscardOldestPolicy: 丢弃队列头的一个任务，并尝试再次提交当前任务
     * DiscardPolicy: 默默地丢弃无法处理的任务，不予任何处理
     * */

    public static class AbortPolicy implements RejectedExecutionHandler {

        public AbortPolicy() {
        }

        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            throw new RejectedExecutionException("Task " + r.toString() + " rejected from " + e.toString());
        }
    }


}
