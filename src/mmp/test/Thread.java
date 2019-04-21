package mmp.test;


public class Thread implements Runnable {

    // 每个线程都有一个自己的ThreadLocal.ThreadLocalMap对象
    // Map中的key为一个ThreadLocal实例，Map使用了弱引用，不过弱引用只针对key，每个key都弱引用指向ThreadLocal实例
    // 当把ThreadLocal实例置为null以后，没有任何强引用指向ThreadLocal实例，所以ThreadLocal实例将会被GC
    // 但是value却不能回收，因为存在一条从current thread过来的强引用
    // 只有当前thread结束以后，current thread就不会存在栈中，强引用断开，Current Thread, Map, value将全部被GC
    ThreadLocal.ThreadLocalMap threadLocals = null;

    public static native Thread currentThread();

    @Override
    public void run() {
    }

    public synchronized void start() {
        // start() --> run()
    }

    public final native boolean isAlive();


    /*
     * 中断：运行时中断，阻塞时中断
     * 阻塞时中断分 2 种。一种是在等待锁的时候中断，一种是进入锁的时候，wait 的时候中断
     * 例如一个 synchronized 同步块，当多线程访问同步块时，同步块外的就是等待锁的状态。
     * 进入锁了，执行 wait 方法，也是阻塞的状态。
     *
     * */


    // 实例方法 设置中断标记，不会中断
    public void interrupt() {
    }

    // 静态方法 判断是否中断，并清除中断状态，第二次调用永远返回 false
    public static boolean interrupted() {
        return currentThread().isInterrupted(true);
    }

    // 实例方法 判断线程是否中断
    public boolean isInterrupted() {
        return isInterrupted(false);
    }

    private native boolean isInterrupted(boolean ClearInterrupted);

    // 暂停当前线程，让出 CPU 给优先级与当前线程相同，或者更高的就绪状态的线程
    // 不会进入到阻塞状态，而是进入到就绪状态
    // 只是让当前线程暂停一下，重新进入就绪的线程池中
    public static native void yield();

    // 只是让出 CPU，不会释放锁
    // 使当前线程进入阻塞状态
    // 由于休眠时间结束后不一定会立即被 CPU 调度，因此线程休眠的时间可能大于传入参数
    public static native void sleep(long millis) throws InterruptedException;

    public static void sleep(long millis, int nanos) throws InterruptedException {
        if (millis < 0) {
            throw new IllegalArgumentException("timeout value is negative");
        }

        if (nanos < 0 || nanos > 999999) {
            throw new IllegalArgumentException("nanosecond timeout value out of range");
        }

        if (nanos >= 500000 || (nanos != 0 && millis == 0)) {
            millis++;
        }

        sleep(millis);
    }


    public final synchronized void join(long millis) throws InterruptedException {
        long base = System.currentTimeMillis();
        long now = 0;

        if (millis < 0) {
            throw new IllegalArgumentException("timeout value is negative");
        }

        if (millis == 0) {
            while (isAlive()) {
                wait(0);
            }
        } else {
            while (isAlive()) {
                long delay = millis - now;
                if (delay <= 0) {
                    break;
                }
                wait(delay);
                now = System.currentTimeMillis() - base;
            }
        }
    }


    // 阻塞
    public final synchronized void join(long millis, int nanos) throws InterruptedException {

        if (millis < 0) {
            throw new IllegalArgumentException("timeout value is negative");
        }

        if (nanos < 0 || nanos > 999999) {
            throw new IllegalArgumentException("nanosecond timeout value out of range");
        }

        if (nanos >= 500000 || (nanos != 0 && millis == 0)) {
            millis++;
        }

        join(millis);
    }

    public final void join() throws InterruptedException {
        join(0);
    }

    // 线程状态
    public enum State {

        // 新建状态
        // new了但是没有启动
        NEW,

        // 可运行状态 start()
        // 可能正在Java虚拟机中运行，也可能正在等待处理器的资源，线程必须获得CPU资源后，才可以运行run()方法，否则排队等待
        RUNNABLE,

        // 阻塞
        // 线程正在等待监视器锁，以便进入一个同步的块/方法
        BLOCKED,


        // 等待
        // 不带超时的Object#wait()
        // 不带超时的Thread#join()
        // LockSupport#park()
        WAITING,


        // 超时等待
        // 带有指定正等待时间的Object#wait()
        // Thread#join()
        // Thread#sleep()
        // LockSupport#parkNanos()
        // LockSupport#parkUntil()
        TIMED_WAITING,

        // 终止
        // 调用终止或者run()方法执行结束后
        TERMINATED;
    }


}
