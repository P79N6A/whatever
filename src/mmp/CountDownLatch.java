package mmp;


import mmp.lock.AbstractQueuedSynchronizer;

import java.util.concurrent.TimeUnit;

// CountDownLatch 共享锁
// 线程共享同一个资源，一旦任意一个线程拿到共享资源，那么所有线程就都拥有的同一份资源
// 在计数为0时唤醒头结点，然后头结点根据FIFO队列唤醒整个节点 setHeadAndPropagate
public class CountDownLatch {

    private final Sync sync;

    // 基于AQS的内部Sync
    private static final class Sync extends AbstractQueuedSynchronizer {

        // 使用AQS的state来表示计数count
        Sync(int count) {
            setState(count);
        }

        // 尝试获取共享锁 如果count=0，即锁是可获取状态
        protected int tryAcquireShared(int acquires) {
            return (getState() == 0) ? 1 : -1;
        }

        // count-1，减过之后，state不是0，就返回false
        protected boolean tryReleaseShared(int releases) {
            for (; ; ) {
                // 获取count
                int c = getState();
                // count已经是0
                if (c == 0) return false;
                // count-1
                int nextc = c - 1;
                // CAS更新同步状态
                if (compareAndSetState(c, nextc)) return nextc == 0;
            }
        }
    }


    public CountDownLatch(int count) {
        if (count < 0) throw new IllegalArgumentException("count < 0");
        this.sync = new Sync(count);
    }


    // 阻塞当前线程直到计数器的数值为0，除非线程被中断
    public void await() throws InterruptedException {
        // 共享式获取AQS的同步状态
        sync.acquireSharedInterruptibly(1);
    }


    // 当前线程在锁存器倒计数至零之前一直等待，除非线程被中断或超出了指定的等待时间
    // 调用AQS的acquireSharedInterruptibly(1)获取共享锁
    // 如果当前线程是中断状态，抛出InterruptedException
    // 否则，调用tryAcquireShared(arg)尝试获取共享锁
    // 尝试成功则返回，否则就调用doAcquireSharedInterruptibly()
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
    }


    // 递减count，如果count=0，则释放所有等待的线程
    // 调用AQS的releaseShared(1)释放共享锁
    // 首先会通过tryReleaseShared()尝试释放共享锁
    // 尝试成功，则直接返回；尝试失败，则通过doReleaseShared()去释放共享锁
    public void countDown() {
        sync.releaseShared(1);
    }

}
