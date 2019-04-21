package mmp;


import mmp.lock.Condition;
import mmp.lock.ReentrantLock;

import java.lang.Thread;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

// 栅栏 屏障
// 通过ReentrantLock独占锁和Condition实现
public class CyclicBarrier {

    private static class Generation {
        boolean broken = false; // 线程组是否发生了异常
    }

    private final ReentrantLock lock = new ReentrantLock();

    private final Condition trip = lock.newCondition();

    private final int parties;

    private final Runnable barrierCommand;

    private Generation generation = new Generation();

    private int count;

    // 在给定数量的参与者处于等待状态时启动，并在启动屏障时执行给定的屏障操作，该操作由最后一个进入屏障的线程执行
    public CyclicBarrier(int parties, Runnable barrierAction) {
        if (parties <= 0) throw new IllegalArgumentException();
        // 同时到达屏障的线程个数
        this.parties = parties;
        // 处在等待状态的线程个数
        this.count = parties;
        // parties个线程到达屏障时，执行的动作
        this.barrierCommand = barrierAction;
    }


    public CyclicBarrier(int parties) {
        this(parties, null);
    }


    private void nextGeneration() {
        // signal completion of last generation
        trip.signalAll();
        // set up next generation
        count = parties;
        generation = new Generation();
    }

    private void breakBarrier() {
        generation.broken = true;
        count = parties;
        trip.signalAll();
    }

    /**
     * Main barrier code, covering the various policies.
     */
    private int dowait(boolean timed, long nanos) throws InterruptedException, BrokenBarrierException, TimeoutException {
        final ReentrantLock lock = this.lock;
        // 获取独占锁
        lock.lock();
        try {
            // 当前代
            final Generation g = generation;
            // 如果这代损坏了，抛出异常
            if (g.broken) throw new BrokenBarrierException();
            // 如果当前线程被中断
            if (Thread.interrupted()) {
                breakBarrier(); // 将损坏状态设置为 true 并通知其他阻塞在此栅栏上的线程
                throw new InterruptedException();
            }
            // 计数器-1
            int index = --count;
            // index=0，则意味着全部线程到达屏障
            if (index == 0) {  // tripped
                boolean ranAction = false;
                try {
                    final Runnable command = barrierCommand;
                    // 执行栅栏任务
                    if (command != null) command.run();
                    ranAction = true;
                    // 唤醒所有等待线程，并换代
                    nextGeneration();
                    return 0;
                } finally {
                    // 如果执行栅栏任务的时候失败了 就将栅栏失效
                    if (!ranAction) breakBarrier();
                }
            }

            // 当前线程阻塞，直到 全部线程到达屏障 || 当前线程被中断 || 超时
            // loop until tripped, broken, interrupted, or timed out
            for (; ; ) {
                try {
                    // 如果没有时间限制 则直接等待 直到被唤醒
                    if (!timed) trip.await(); // 这里会释放锁
                        // 如果有时间限制 则等待指定时间
                    else if (nanos > 0L) nanos = trip.awaitNanos(nanos);
                } catch (InterruptedException ie) {
                    // 如果等待过程中，线程被中断
                    // 当前代 && 没有损坏
                    if (g == generation && !g.broken) {
                        // 让栅栏失效
                        breakBarrier();
                        throw ie;
                    } else {
                        // 这个线程不是这代的，不会影响当前这代栅栏执行逻辑，打个标记就好了
                        Thread.currentThread().interrupt();
                    }
                }
                // 任何一个线程中断了 会调用 breakBarrier 唤醒其他的线程
                // 其他线程醒来后 也要抛出异常
                if (g.broken) throw new BrokenBarrierException();

                // 一个线程可以使用多个栅栏，当别的栅栏唤醒了这个线程，需要判断是否是当前代
                // 如果正常换代，则返回当前线程所在栅栏的下标
                if (g != generation) return index;

                // 如果是超时等待，并且时间已到，则通过breakBarrier()终止屏障，唤醒所有等待线程
                if (timed && nanos <= 0L) {
                    breakBarrier();
                    throw new TimeoutException();
                }
            }
        } finally {
            // 释放独占锁
            lock.unlock();
        }
    }


    // 在所有参与者都已经在此屏障上调用 await 方法之前，将一直阻塞
    public int await() throws InterruptedException, BrokenBarrierException {
        try {
            return dowait(false, 0L);
        } catch (TimeoutException toe) {
            throw new Error(toe); // cannot happen
        }
    }

    public int await(long timeout, TimeUnit unit) throws InterruptedException, BrokenBarrierException, TimeoutException {
        return dowait(true, unit.toNanos(timeout));
    }


}
