package mmp;

import mmp.lock.Condition;
import mmp.lock.Lock;
import mmp.lock.ReentrantLock;

public class ProductQueue<T> {

    private final T[] items;

    private final Lock lock = new ReentrantLock();

    private Condition notFull = lock.newCondition();

    private Condition notEmpty = lock.newCondition();

    private int head, tail, count;

    public ProductQueue(int maxSize) {
        items = (T[]) new Object[maxSize];
    }

    public ProductQueue() {
        this(Integer.MAX_VALUE);
    }

    public void put(T t) throws InterruptedException {
        lock.lock();
        try {
            while (count == getCapacity()) notFull.await(); // 数组满了 释放锁，等待
            items[tail] = t;  // 放入数据
            if (++tail == getCapacity()) tail = 0; // 到最后一个位置了 下标从头开始 防止下标越界
            ++count;
            notEmpty.signalAll(); // 通知 take 线程
        } finally {
            lock.unlock();
        }
    }

    public T take() throws InterruptedException {
        lock.lock();
        try {
            while (count == 0) notEmpty.await(); // 如果数组没有数据 等待
            T ret = items[head]; // 取数据
            items[head] = null; // GC
            if (++head == getCapacity()) head = 0; // 如果到数组尽头了 从头开始
            --count;
            notFull.signalAll(); // 通知阻塞的 put 线程
            return ret;
        } finally {
            lock.unlock();
        }
    }

    public int getCapacity() {
        return items.length;
    }

    public int size() {
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }
}
