package mmp;


import mmp.lock.AbstractQueuedSynchronizer;

import java.util.concurrent.TimeUnit;


/*
* Semaphore 共享锁，通过设置 state 变量实现
* acquire ，state--
* release ，state++
* state == 0 ，AQS 中阻塞
*
*
* Lock#unlock之前，线程必须事先持有这个锁（Lock#lock）
* Semaphore#release之前不要求先Semaphore#acquire
* 可以通过非Owner线程实现死锁恢复
*
*
* 公平信号量和非公平信号量的释放机制一样，不同的是获取机制
* 非公平信号量：信号量足够的时候，无论当前线程是不是在CLH队列的头部，都会直接获取信号量，吞吐量较大，但可能存在饥饿现象
* 公平信号量：如果当前线程不在CLH队列的头部，则排队等候，不存在饥饿现象
* 具体体现在tryAcquireShared()的实现不同
*
* 应用 连接池 对象池 缓存池
* */
public class Semaphore {

    private final Sync sync;

    abstract static class Sync extends AbstractQueuedSynchronizer {

        Sync(int permits) {
            setState(permits);
        }

        // 返回剩余 state 大小
        // 当返回值小于 0 的时候，说明获取锁失败了，需要进入AQS等待队列
        final int nonfairTryAcquireShared(int acquires) {
            for (; ; ) {
                // 信号量的许可数
                int available = getState();
                // 获得acquires个信号量许可后，剩余的信号量许可数
                int remaining = available - acquires;
                // remaining < 0，获取锁失败
                // remaining > 0，循环尝试用 CAS 将 state 变量更新成 remaining
                if (remaining < 0 || compareAndSetState(available, remaining)) return remaining;
            }
        }

        protected final boolean tryReleaseShared(int releases) {
            for (; ; ) {
                // 可以获得的信号量的许可数
                int current = getState();
                // 释放releases个许可之后，剩余许可数
                int next = current + releases;
                if (next < current) throw new Error("Maximum permit count exceeded");
                // 循环尝试 CAS设置可获得的许可数为next
                if (compareAndSetState(current, next)) return true;
            }
        }


    }


    static final class NonfairSync extends Sync {

        NonfairSync(int permits) {
            super(permits);
        }

        protected int tryAcquireShared(int acquires) {
            return nonfairTryAcquireShared(acquires);
        }
    }


    static final class FairSync extends Sync {

        FairSync(int permits) {
            super(permits);
        }

        // 尝试获取acquires个许可
        protected int tryAcquireShared(int acquires) {
            for (; ; ) {
                // 如果当前线程是CLH队列中的第一个节点
                if (hasQueuedPredecessors()) return -1;
                // 可以获得的许可数
                int available = getState();
                // 获得acquires个许可之后，剩余的许可数
                int remaining = available - acquires;

                if (remaining < 0 || compareAndSetState(available, remaining)) return remaining;
            }
        }
    }


    // 许可数 默认非公平
    // 信号量为1时 相当于普通的锁
    // 信号量大于1时 共享锁
    public Semaphore(int permits) {
        sync = new NonfairSync(permits);
    }

    public Semaphore(int permits, boolean fair) {
        sync = fair ? new FairSync(permits) : new NonfairSync(permits);
    }


    // 获取一个许可，在提供许可前一直阻塞，除非中断
    public void acquire() throws InterruptedException {
        // 尝试获取一个锁
        sync.acquireSharedInterruptibly(1);
    }


    // 从信号量获取给定数目的许可
    public void acquire(int permits) throws InterruptedException {
        if (permits < 0) throw new IllegalArgumentException();
        sync.acquireSharedInterruptibly(permits);
    }


    public void acquireUninterruptibly() {
        sync.acquireShared(1);
    }


    public void acquireUninterruptibly(int permits) {
        if (permits < 0) throw new IllegalArgumentException();
        sync.acquireShared(permits);
    }


    // 调用时存在一个可用许可，才从信号量获取许可 非阻塞
    public boolean tryAcquire() {
        return sync.nonfairTryAcquireShared(1) >= 0;
    }


    public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
    }

    // 调用时有给定数目的许可时，才获取许可
    public boolean tryAcquire(int permits) {
        if (permits < 0) throw new IllegalArgumentException();
        return sync.nonfairTryAcquireShared(permits) >= 0;
    }

    public boolean tryAcquire(int permits, long timeout, TimeUnit unit) throws InterruptedException {
        if (permits < 0) throw new IllegalArgumentException();
        return sync.tryAcquireSharedNanos(permits, unit.toNanos(timeout));
    }


    // 释放一个许可，返回给信号量
    // 如果没有获取许可，直接release许可会导致允许的同时线程数+1，实际上是调用AQS中的releaseShared()
    public void release() {
        sync.releaseShared(1);
    }


    // 释放给定数目的许可，返回到信号量
    public void release(int permits) {
        if (permits < 0) throw new IllegalArgumentException();
        sync.releaseShared(permits);
    }


}
