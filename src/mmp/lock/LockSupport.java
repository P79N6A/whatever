package mmp.lock;


public class LockSupport {
    private LockSupport() {
    } // Cannot be instantiated.


    public static Object getBlocker(Thread t) {
        if (t == null) throw new NullPointerException();
        return UNSAFE.getObjectVolatile(t, parkBlockerOffset);
    }

    private static void setBlocker(Thread t, Object arg) {
        // Even though volatile, hotspot doesn't need a write barrier here.
        UNSAFE.putObject(t, parkBlockerOffset, arg);
    }

    // 如果给定线程的许可尚不可用，则使其可用
    public static void unpark(Thread thread) {
        if (thread != null)
            UNSAFE.unpark(thread); // unpark函数为线程提供 permit ，线程调用park函数则等待 permit
    }

    // 在许可可用前挂起当前线程
    public static void park(Object blocker) {
        Thread t = Thread.currentThread();
        setBlocker(t, blocker);
        // 阻塞线程
        UNSAFE.park(false, 0L);
        setBlocker(t, null);
    }

    // 在许可可用前挂起当前线程，并最多等待指定的等待时间
    public static void parkNanos(Object blocker, long nanos) {
        if (nanos > 0) {
            Thread t = Thread.currentThread();
            setBlocker(t, blocker);
            UNSAFE.park(false, nanos);
            setBlocker(t, null);
        }
    }


    // 在指定的时限前挂起当前线程，除非许可可用
    public static void parkUntil(Object blocker, long deadline) {
        Thread t = Thread.currentThread();
        setBlocker(t, blocker);
        UNSAFE.park(true, deadline);
        setBlocker(t, null);
    }

    // 挂起当前线程，除非许可可用
    // 以下几种情况时，线程会被唤醒：
    // Some other thread invokes {@link #unpark unpark} with the current thread as the target （调用unpark方法）
    // Some other thread {@linkplain Thread#interrupt interrupts} the current thread （被中断interrupts）
    // The call spuriously (that is, for no reason) returns.（posix condition里的”Spurious wakeup”）
    public static void park() {
        // isAbsolute代表传入的time是绝对时间还是相对时间
        UNSAFE.park(false, 0L);
    }


    private static final sun.misc.Unsafe UNSAFE;
    private static final long parkBlockerOffset;
    private static final long SEED;
    private static final long PROBE;
    private static final long SECONDARY;

    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> tk = Thread.class;
            parkBlockerOffset = UNSAFE.objectFieldOffset(tk.getDeclaredField("parkBlocker"));
            SEED = UNSAFE.objectFieldOffset(tk.getDeclaredField("threadLocalRandomSeed"));
            PROBE = UNSAFE.objectFieldOffset(tk.getDeclaredField("threadLocalRandomProbe"));
            SECONDARY = UNSAFE.objectFieldOffset(tk.getDeclaredField("threadLocalRandomSecondarySeed"));
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

}







