package mmp.lock;

import java.util.concurrent.TimeUnit;

/**
 * 可重入 互斥 独占锁
 */
public class ReentrantLock implements Lock {

    private final Sync sync;

    abstract static class Sync extends AbstractQueuedSynchronizer {

        abstract void lock();

        /**
         * 尝试释放锁
         */
        @Override
        protected final boolean tryRelease(int releases) {
            // releases对于独占锁是1
            int c = getState() - releases; // 可重入，要释放重入获取的锁
            // 如果当前线程没持有该锁
            if (Thread.currentThread() != getExclusiveOwnerThread())
                throw new IllegalMonitorStateException();
            boolean free = false;

            // 如果剩余c==0（没有线程持有锁），当前线程就是最后一个持有锁的线程，唤醒下一个需要锁的AQS节点
            // 否则就只是减少锁持有的计数器
            if (c == 0) {
                // 只有全部释放才返回true
                free = true;
                // 设置锁的持有者为null，同步队列的节点可以去获取同步状态了
                setExclusiveOwnerThread(null);
            }

            // 设置锁状态
            setState(c);
            return free;
        }

        /**
         * 相比较公平锁的 tryAcquire方法，少了hasQueuedPredecessors
         * 直接抢锁，抢不到再进入队列，等待他的前继节点唤醒
         */
        final boolean nonfairTryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            // 如果锁空闲
            if (c == 0) {
                // 获取到同步状态
                if (compareAndSetState(0, acquires)) {
                    // 设置当前线程占有锁
                    setExclusiveOwnerThread(current);
                    return true;
                }

            }
            // 线程已经占有锁了 重入
            else if (current == getExclusiveOwnerThread()) {
                // 同步状态记录重入的次数
                int nextc = c + acquires;
                if (nextc < 0)
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);
                return true;
            }
            return false;
        }

        @Override
        protected final boolean isHeldExclusively() {
            return getExclusiveOwnerThread() == Thread.currentThread();
        }

        final ConditionObject newCondition() {
            return new ConditionObject();
        }

    }

    static final class NonfairSync extends Sync {

        /**
         * 非公平锁中，抢到AQS的同步状态的未必是FIFO同步队列的首节点
         * 只要通过CAS抢到了同步状态或在acquire中抢到同步状态，就优先占有锁
         */
        @Override
        final void lock() {
            // CAS设置同步状态，成功就设置锁的持有线程为自己 非公平
            if (compareAndSetState(0, 1))
                setExclusiveOwnerThread(Thread.currentThread());
                // 获取失败 进入AQS同步队列排队 执行AQS的acquire方法 公平
            else
                acquire(1);
        }

        @Override
        protected final boolean tryAcquire(int acquires) {
            return nonfairTryAcquire(acquires);
        }
    }

    static final class FairSync extends Sync {
        @Override
        final void lock() {
            // 按AQS的同步队列去获取同步状态 公平
            // 公平锁直接调用AQS的acquire方法，acquire中调用tryAcquire
            acquire(1);

        }

        /**
         * 和非公平锁相比，这里不会CAS，之后tryAcquire抢锁时，也会先调用hasQueuedPredecessors确定是否有前继节点等待
         */
        @Override
        protected final boolean tryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            // 如果锁空闲
            if (c == 0) {
                // 没有前驱节点（公平） && CAS设置state成功
                if (!hasQueuedPredecessors() && compareAndSetState(0, acquires)) {
                    // 设置当前线程为锁的持有线程
                    setExclusiveOwnerThread(current);
                    // 抢锁成功
                    return true;
                }
            }
            // 如果锁不空闲，但当前线程和锁的持有线程相同 重入
            else if (current == getExclusiveOwnerThread()) {
                // 重入次数
                int nextc = c + acquires;
                if (nextc < 0)
                    throw new Error("Maximum lock count exceeded");
                // 不用compareAndSetState，因为独占锁是当前线程，不需要CAS
                setState(nextc);
                return true;
            }
            return false;
        }
    }

    /**
     * 默认非公平锁
     */
    public ReentrantLock() {
        sync = new NonfairSync();
    }

    public ReentrantLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
    }

    /**
     * 获取锁 阻塞
     */
    @Override
    public void lock() {
        sync.lock();
    }

    /**
     * 获取锁 阻塞 可以响应中断而停止阻塞返回
     */
    @Override
    public void lockInterruptibly() throws InterruptedException {
        // 调用AQS#acquireInterruptibly
        sync.acquireInterruptibly(1);
    }

    /**
     * 获取锁 非阻塞 返回boolean
     */
    @Override
    public boolean tryLock() {
        return sync.nonfairTryAcquire(1);
    }

    @Override
    public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(timeout));
    }

    /**
     * 释放锁
     */
    @Override
    public void unlock() {
        sync.release(1);
    }

    @Override
    public Condition newCondition() {
        return sync.newCondition();
    }

}





