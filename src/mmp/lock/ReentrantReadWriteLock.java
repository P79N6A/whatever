package mmp.lock;

import java.util.concurrent.TimeUnit;

public class ReentrantReadWriteLock implements ReadWriteLock {

    private final ReentrantReadWriteLock.ReadLock readerLock;

    private final ReentrantReadWriteLock.WriteLock writerLock;

    final Sync sync;


    public ReentrantReadWriteLock() {
        this(false);
    }


    public ReentrantReadWriteLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
        readerLock = new ReadLock(this);
        writerLock = new WriteLock(this);
    }

    public ReentrantReadWriteLock.WriteLock writeLock() {
        return writerLock;
    }

    public ReentrantReadWriteLock.ReadLock readLock() {
        return readerLock;
    }


    abstract static class Sync extends AbstractQueuedSynchronizer {

        // 2^16-1
        // 读写锁 state int 共4个字节32位，读写状态各占16位，高16位表示读，低16位表示写
        // 写状态 state & 0x0000FFFF
        // 读状态 state >>> 16
        // 写状态 + 1 state + 1，
        // 读状态 + 1 state + (1 << 16)

        /*
         * 读锁是可共享的，写锁是互斥的
         * 写锁，用低16位表示线程的重入次数
         * 读锁，可以同时有多个线程，重入次数通过ThreadLocal变量来记录
         * */
        static final int SHARED_SHIFT = 16;
        static final int SHARED_UNIT = (1 << SHARED_SHIFT);
        static final int MAX_COUNT = (1 << SHARED_SHIFT) - 1;
        static final int EXCLUSIVE_MASK = (1 << SHARED_SHIFT) - 1;


        /*
         * 无符号位移（>>>）和有符号位移（>>）
         * 有符号位移时如果为正数时位移后前面补0，为负数时则位移后前面补1
         * */

        // 15>>>2
        // 00000000 00000000 00000000 00001111 ->
        // 00000000 00000000 00000000 00000011 ->
        // 3

        // 15>>2
        // 00000000 00000000 00000000 00001111 ->
        // 00000000 00000000 00000000 00000011 ->
        // 3

        // -15>>>2
        // 11111111 11111111 11111111 11110001 ->
        // 00111111 11111111 11111111 11111100 ->
        // 2^2+2^3+2^4+...+2^30=1073741820

        // -15>>2
        // 11111111 11111111 11111111 11110001 ->
        // 11111111 11111111 11111111 11111100 ->
        // 补码（负数的二进制）=反码+1
        // -1：11111111 11111111 11111111 11111011 ->
        // 反码：00000000 00000000 00000000 00000100 ->
        // -4

        // 当需要移位的数为正数时，有符号位移（>>）和无符号位移（>>>）是相同的。
        // 当需要移位的数为负数时，有符号位移（>>）的结果 还为负数，无符号位移（>>>）的结果为正数

        static int sharedCount(int c) {
            return c >>> SHARED_SHIFT;
        }


        static int exclusiveCount(int c) {
            return c & EXCLUSIVE_MASK;
        }

        // 线程读锁的count
        static final class HoldCounter {
            int count = 0;
            // Use id, not reference, to avoid garbage retention
            final long tid = getThreadId(Thread.currentThread());
        }


        // ThreadLocal
        static final class ThreadLocalHoldCounter extends ThreadLocal<HoldCounter> {
            public HoldCounter initialValue() {
                return new HoldCounter();
            }
        }

        // 每个线程持有一份ThreadLocal
        private transient ThreadLocalHoldCounter readHolds;

        // 上一个获取读锁的线程重复获取读锁，或者释放读锁，就会直接使用这个变量，减少ThreadLocal.get()的次数
        private transient HoldCounter cachedHoldCounter;

        // 第一个获取读锁的线程
        private transient Thread firstReader = null;
        // firstReader的计数器
        private transient int firstReaderHoldCount;

        Sync() {
            readHolds = new ThreadLocalHoldCounter();
            setState(getState()); // ensures visibility of readHolds
        }


        abstract boolean readerShouldBlock();


        abstract boolean writerShouldBlock();


        protected final boolean tryRelease(int releases) {
            // 是否持有写锁
            if (!isHeldExclusively()) throw new IllegalMonitorStateException();
            // 重入次数减少
            int nextc = getState() - releases;
            // 减少到0写锁释放
            boolean free = exclusiveCount(nextc) == 0;
            // 释放成功，设置持有锁的线程为null
            if (free) setExclusiveOwnerThread(null);
            // 设置 state
            setState(nextc);
            return free;
        }


        protected final boolean tryAcquire(int acquires) {

            Thread current = Thread.currentThread();

            int c = getState();
            // 写状态 低16位 state & 0x00001111
            int w = exclusiveCount(c);
            // 锁不空闲（读或写）
            if (c != 0) {
                // 写状态为0，写锁空闲，但同步状态不为0，说明有读锁，获取锁失败（有读锁，不能获取写锁）
                // 写状态不为0，但其他写线程获取了锁，获取锁失败
                if (w == 0 || current != getExclusiveOwnerThread()) return false;
                // 写锁重入次数超过最大值
                if (w + exclusiveCount(acquires) > MAX_COUNT) throw new Error("Maximum lock count exceeded");
                // 更新锁状态
                setState(c + acquires);
                return true;
            }

            // state是0时，表示可以获取锁
            // writerShouldBlock 判断是否需要锁
            // 非公平，返回false，尝试用CAS修改state
            // 公平，hasQueuedPredecessors判断同步队列中是否有前驱节点等待，没有就返回false，尝试用CAS修改state
            if (writerShouldBlock() || !compareAndSetState(c, c + acquires)) return false;
            // 获取写锁成功，独占
            // 成功修改state后，修改锁的持有线程
            setExclusiveOwnerThread(current);
            return true;
        }


        // 获取读锁（共享锁）
        protected final int tryAcquireShared(int unused) {
            Thread current = Thread.currentThread();
            // 获取锁状态
            int c = getState();

            // 写锁被持有 && 持有该锁的线程不是当前线程 失败
            if (exclusiveCount(c) != 0 && getExclusiveOwnerThread() != current) return -1;
            // 获取读锁计数
            int r = sharedCount(c);
            // 不需要阻塞等待 && 读锁共享计数<MAX_COUNT && CAS函数更新锁的状态+1成功
            if (!readerShouldBlock() && r < MAX_COUNT && compareAndSetState(c, c + SHARED_UNIT)) {
                // 读锁空闲
                if (r == 0) {
                    // 将当前线程设置为第一个获取读锁的线程
                    firstReader = current;
                    // 计数器设为1
                    firstReaderHoldCount = 1;
                }
                // 第一个获取读锁的线程重入
                else if (firstReader == current) {
                    // 将计数器加一
                    firstReaderHoldCount++;
                }
                // 非第一个获取读锁的线程
                else {
                    HoldCounter rh = cachedHoldCounter;
                    // 上一个线程计数器是null || 不是当前线程
                    // 给当前线程新建一个HoldCounter
                    if (rh == null || rh.tid != getThreadId(current)) cachedHoldCounter = rh = readHolds.get();
                        // 如果不是null，且count是 0，就将上个线程的HoldCounter覆盖本地的
                    else if (rh.count == 0) readHolds.set(rh);
                    // 该线程获取读锁计数++
                    rh.count++;
                }
                return 1;
            }
            // 获取读锁失败，循环重试
            return fullTryAcquireShared(current);
        }

        final int fullTryAcquireShared(Thread current) {

            HoldCounter rh = null;
            for (; ; ) {
                int c = getState();
                // 写锁不空闲
                if (exclusiveCount(c) != 0) {
                    // 持有写锁的不是当前线程
                    if (getExclusiveOwnerThread() != current) return -1;
                }

                // 如果需要阻塞等待

                // 写锁空闲，且可以获取读锁，需要阻塞等待
                else if (readerShouldBlock()) {

                    // 第一个获取读锁的线程是当前线程
                    if (firstReader == current) {
                        // assert firstReaderHoldCount > 0;
                    }
                    // 不是当前线程
                    else {
                        if (rh == null) {
                            // 上一个获取读锁的线程的rh
                            rh = cachedHoldCounter;
                            // null || rh 不是当前线程的
                            if (rh == null || rh.tid != getThreadId(current)) {
                                // 从ThreadLocal中取出计数器
                                rh = readHolds.get();
                                if (rh.count == 0) readHolds.remove();
                            }
                        }
                        // 如果当前线程获取锁的计数=0，则返回-1
                        if (rh.count == 0) return -1;
                    }
                }
                // 如果不需要阻塞等待
                // 如果读锁计数超过MAX_COUNT，则抛出异常
                if (sharedCount(c) == MAX_COUNT) throw new Error("Maximum lock count exceeded");
                // CAS尝试对state加65536，设置读锁，高16位加一
                if (compareAndSetState(c, c + SHARED_UNIT)) {
                    // 如果读锁空闲
                    if (sharedCount(c) == 0) {
                        // 设置第一个读锁
                        firstReader = current;
                        // 计数器为 1
                        firstReaderHoldCount = 1;
                    }
                    // 读锁不空闲，第一个获取读锁的线程是当前线程
                    else if (firstReader == current) {
                        firstReaderHoldCount++;
                    }
                    // 不是当前线程
                    else {
                        if (rh == null) rh = cachedHoldCounter;
                        if (rh == null || rh.tid != getThreadId(current)) rh = readHolds.get();
                        else if (rh.count == 0) readHolds.set(rh);
                        rh.count++;
                        cachedHoldCounter = rh; // cache for release
                    }
                    return 1;
                }
            }
        }

        // 尝试释放共享锁
        protected final boolean tryReleaseShared(int unused) {
            Thread current = Thread.currentThread();

            // 当前线程是第一个获取读锁的线程
            if (firstReader == current) {
                // 获取锁的次数==1，第一次占有读锁，直接清除该线程
                if (firstReaderHoldCount == 1) firstReader = null;
                    // 减少第一个获取读锁的线程的重入次数
                else firstReaderHoldCount--;
            }
            // 如果不是
            else {
                // 当前线程持有共享锁（读锁）的数量，包括重入的数量
                HoldCounter rh = cachedHoldCounter;
                // 缓存是null || 缓存所属线程不是当前线程
                if (rh == null || rh.tid != getThreadId(current)) rh = readHolds.get(); // 当前线程不是上一个读锁的线程，从ThreadLocal取
                int count = rh.count;
                // 计数器<= 1，直接删除计数器
                if (count <= 1) {
                    readHolds.remove(); // 读锁释放
                    if (count <= 0) throw unmatchedUnlockException();
                }
                --rh.count;  // 重入次数减少
            }
            for (; ; ) {
                int c = getState();
                // 锁的获取次数-
                int nextc = c - SHARED_UNIT;
                // CAS更新锁的状态，减少读锁的线程数量
                // 修改成功后，如果是0，表示读锁和写锁都空闲，则可以唤醒后面的等待线程
                if (compareAndSetState(c, nextc)) return nextc == 0;
            }
        }


        private IllegalMonitorStateException unmatchedUnlockException() {
            return new IllegalMonitorStateException("attempt to unlock read lock, not locked by current thread");
        }


        final boolean tryWriteLock() {
            Thread current = Thread.currentThread();
            int c = getState();
            if (c != 0) {
                int w = exclusiveCount(c);
                if (w == 0 || current != getExclusiveOwnerThread()) return false;
                if (w == MAX_COUNT) throw new Error("Maximum lock count exceeded");
            }
            if (!compareAndSetState(c, c + 1)) return false;
            setExclusiveOwnerThread(current);
            return true;
        }

        final boolean tryReadLock() {
            Thread current = Thread.currentThread();
            for (; ; ) {
                int c = getState();
                if (exclusiveCount(c) != 0 && getExclusiveOwnerThread() != current) return false;
                int r = sharedCount(c);
                if (r == MAX_COUNT) throw new Error("Maximum lock count exceeded");
                if (compareAndSetState(c, c + SHARED_UNIT)) {
                    if (r == 0) {
                        firstReader = current;
                        firstReaderHoldCount = 1;
                    } else if (firstReader == current) {
                        firstReaderHoldCount++;
                    } else {
                        HoldCounter rh = cachedHoldCounter;
                        if (rh == null || rh.tid != getThreadId(current)) cachedHoldCounter = rh = readHolds.get();
                        else if (rh.count == 0) readHolds.set(rh);
                        rh.count++;
                    }
                    return true;
                }
            }
        }

        protected final boolean isHeldExclusively() {
            return getExclusiveOwnerThread() == Thread.currentThread();
        }

        // Methods relayed to outer class

        final ConditionObject newCondition() {
            return new ConditionObject();
        }


    }


    static final class NonfairSync extends Sync {

        final boolean writerShouldBlock() {
            return false; // writers can always barge
        }

        final boolean readerShouldBlock() {

            return apparentlyFirstQueuedIsExclusive();
        }
    }


    static final class FairSync extends Sync {

        final boolean writerShouldBlock() {
            return hasQueuedPredecessors();
        }

        final boolean readerShouldBlock() {
            return hasQueuedPredecessors();
        }
    }


    public static class ReadLock implements Lock {

        private final Sync sync;


        protected ReadLock(ReentrantReadWriteLock lock) {
            sync = lock.sync;
        }


        public void lock() {
            sync.acquireShared(1);
        }


        public void lockInterruptibly() throws InterruptedException {
            sync.acquireSharedInterruptibly(1);
        }


        public boolean tryLock() {
            return sync.tryReadLock();
        }


        public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
            return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
        }


        public void unlock() {
            sync.releaseShared(1);
        }


        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }


    }


    public static class WriteLock implements Lock {

        private final Sync sync;


        protected WriteLock(ReentrantReadWriteLock lock) {
            sync = lock.sync;
        }


        public void lock() {
            sync.acquire(1);
        }


        public void lockInterruptibly() throws InterruptedException {
            sync.acquireInterruptibly(1);
        }


        public boolean tryLock() {
            return sync.tryWriteLock();
        }


        public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
            return sync.tryAcquireNanos(1, unit.toNanos(timeout));
        }


        public void unlock() {
            sync.release(1);
        }


        public Condition newCondition() {
            return sync.newCondition();
        }


    }

    // Instrumentation and status

    static final long getThreadId(Thread thread) {
        return UNSAFE.getLongVolatile(thread, TID_OFFSET);
    }

    // Unsafe mechanics
    private static final sun.misc.Unsafe UNSAFE;
    private static final long TID_OFFSET;

    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> tk = Thread.class;
            TID_OFFSET = UNSAFE.objectFieldOffset(tk.getDeclaredField("tid"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }


    public static void main(String[] args) {

        ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

        boolean cacheValid = false;

        rwl.readLock().lock();
        if (!cacheValid) {
            // Must release read lock before acquiring write lock
            rwl.readLock().unlock();
            rwl.writeLock().lock();
            try {
                // Recheck state because another thread might have
                // acquired write lock and changed state before we did.
                if (!cacheValid) {
                    // data = ...
                    cacheValid = true;
                }
                // Downgrade by acquiring read lock before releasing write lock
                rwl.readLock().lock();
            } finally {
                rwl.writeLock().unlock(); // Unlock write, still hold read
            }
        }

        try {
            // use(data);
        } finally {
            rwl.readLock().unlock();
        }


    }

}




