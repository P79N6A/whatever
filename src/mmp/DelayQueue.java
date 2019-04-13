package mmp;

import mmp.lock.Condition;
import mmp.lock.ReentrantLock;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

// 无界
// DelayQueue = BlockingQueue + PriorityQueue + Delayed
// 应用 ScheduledThreadPoolExecutor
public class DelayQueue<E extends Delayed> extends AbstractQueue<E> implements BlockingQueue<E> {
    private final transient ReentrantLock lock = new ReentrantLock();

    // 内部用于存储对象
    private final PriorityQueue<E> q = new PriorityQueue<>();

    public int size() {
        return 0;
    }

    public int drainTo(Collection<? super E> c) {
        return 0;
    }

    public Iterator<E> iterator() {
        return Collections.emptyIterator();
    }

    // 当存在多个take线程时，同时只生效一个leader线程
    private Thread leader = null;

    private final Condition available = lock.newCondition();

    public DelayQueue() {
    }

    public DelayQueue(Collection<? extends E> c) {
        this.addAll(c);
    }


    public boolean add(E e) {
        return offer(e);
    }

    public void put(E e) {
        offer(e);
    }

    public boolean offer(E e) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            q.offer(e);
            // 添加元素后peek还是e，重置leader，通知条件队列
            if (q.peek() == e) {
                leader = null;
                available.signal();
            }
            return true;
        } finally {
            lock.unlock();
        }
    }


    public E poll() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            E first = q.peek();
            // 队列为空或者延迟时间未过期
            if (first == null || first.getDelay(TimeUnit.NANOSECONDS) > 0) return null;
            else return q.poll();
        } finally {
            lock.unlock();
        }
    }


    // take元素，元素未过期需要阻塞
    public E take() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            for (; ; ) {
                E first = q.peek();
                // 队列空，等待
                if (first == null) available.await();
                else {
                    // 剩余延迟时间
                    long delay = first.getDelay(TimeUnit.NANOSECONDS);
                    // 到期，poll
                    if (delay <= 0)
                        return q.poll();
                    first = null; // don't retain ref while waiting
                    // 当leader存在时，说明有线程在take了，其它的take线程均为follower，等待
                    if (leader != null) available.await();
                        // 当leader不存在时，说明没有其他线程take，当前线程即成为leader，在delay之后，将leader释放
                    else {
                        Thread thisThread = Thread.currentThread();
                        leader = thisThread; // 设置当前为leader等待
                        try {
                            available.awaitNanos(delay); // 等待指定时间
                        } finally {
                            // 检查是否被其他线程改变，没有就重置，再次循环
                            if (leader == thisThread) leader = null;
                        }
                    }
                }
            }
        } finally {
            // 最后如果队列还有内容，且leader空缺，则signal唤醒挂起的take线程，其中之一将成为新的leader
            if (leader == null && q.peek() != null) available.signal();
            lock.unlock();
        }
    }

    /*
     * 关于 first = null
     * 参考 https://www.jianshu.com/p/e0bcc9eae0ae
     * 线程A进来take，然后进入 else 的else，设置了leader为当前线程A
     * 线程B进来take，进入else的阻塞，如果他持有first引用
     * 如果线程A阻塞完毕，take成功，这个first对象理应被GC，但它还被线程B持有着，GC可达，所以不能回收
     * 假设还有线程C 、D、E 持有引用，那么无限期的不能回收该引用，造成内存泄露
     * */

    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            for (; ; ) {
                E first = q.peek();
                if (first == null) {
                    if (nanos <= 0) return null;
                    else nanos = available.awaitNanos(nanos);
                } else {
                    long delay = first.getDelay(TimeUnit.NANOSECONDS);
                    if (delay <= 0) return q.poll();
                    if (nanos <= 0) return null;
                    first = null; // don't retain ref while waiting
                    if (nanos < delay || leader != null) nanos = available.awaitNanos(nanos);
                    else {
                        Thread thisThread = Thread.currentThread();
                        leader = thisThread;
                        try {
                            long timeLeft = available.awaitNanos(delay);
                            nanos -= delay - timeLeft;
                        } finally {
                            if (leader == thisThread) leader = null;
                        }
                    }
                }
            }
        } finally {
            if (leader == null && q.peek() != null) available.signal();
            lock.unlock();
        }
    }


    public E peek() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return q.peek();
        } finally {
            lock.unlock();
        }
    }
}
