package mmp.container;


import mmp.lock.Condition;
import mmp.lock.ReentrantLock;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;


/*
 * 数组 有界 大小固定 阻塞 队列 FIFO 不接受 null 元素
 * 使用可重入锁ReentrantLock控制队列的访问，两个Condition实现生产者-消费者模型
 * */
public class ArrayBlockingQueue<E> extends AbstractQueue<E> {


    // 元素
    final Object[] items;


    // take, poll, peek or remove的下一个索引（等待插入的数组下标位置）
    int takeIndex;


    // put, offer, or add的下一个索引
    int putIndex;


    // 队列中元素个数
    int count;

    // 可重入锁 独占锁 出队入队同一个锁
    final ReentrantLock lock;


    // 队列不为空的条件
    private final Condition notEmpty;


    // 队列未满的条件
    private final Condition notFull;


    transient Itrs itrs = null;


    // 返回对应索引上的元素
    final E itemAt(int i) {
        return (E) items[i];
    }


    private static void checkNotNull(Object v) {
        if (v == null) throw new NullPointerException();
    }


    /*
     * 直接返回count变量
     * ConcurrentLinkedQueue的size则会遍历队列，故ArrayBlockingQueue方法效率高
     * ConcurrentLinkedQueue的size方法没有加锁，可能不准确
     */
    public int size() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }


    // 元素入队，加锁
    // 没有内存不可见问题，释放锁后修改的共享变量值会刷新主内存
    private void enqueue(E x) {
        final Object[] items = this.items;
        // 将新元素插入队列中
        items[putIndex] = x;
        // 数组下标向后移动，如果队列满则将putIndex置为0
        if (++putIndex == items.length) putIndex = 0;
        // 元素个数+1
        count++;
        // 元素加入成功，队列不为空，唤醒非空等待队列中的线程
        notEmpty.signal();
    }


    // 元素出队，加锁
    private E dequeue() {
        final Object[] items = this.items;
        @SuppressWarnings("unchecked")
        E x = (E) items[takeIndex]; // 取出队首元素
        items[takeIndex] = null; // 置为null，GC
        if (++takeIndex == items.length) takeIndex = 0;
        count--; // 元素个数-1
        if (itrs != null) itrs.elementDequeued();
        notFull.signal(); // 元素取出成功，队列不满，唤醒非满等待队列线程
        return x;
    }

    // 默认非公平锁
    // 数组大小不可改变
    public ArrayBlockingQueue(int capacity) {
        this(capacity, false);
    }


    public ArrayBlockingQueue(int capacity, boolean fair) {
        if (capacity <= 0) throw new IllegalArgumentException();
        this.items = new Object[capacity];
        lock = new ReentrantLock(fair);
        notEmpty = lock.newCondition();
        notFull = lock.newCondition();
    }


    // 构造函数，带有初始内容的队列
    public ArrayBlockingQueue(int capacity, boolean fair, Collection<? extends E> c) {
        this(capacity, fair);
        final ReentrantLock lock = this.lock;
        lock.lock(); // Lock only for visibility, not mutual exclusion
        // 这个锁不是为了互斥，而是保证可见性
        // 线程T1使用集合C实例化对象，然后T2对实例化的对象做入队操作
        // 如果不加锁（加锁会保证其可见性，写回主存），T1的集合C可能只存在T1线程维护的缓存中，并没有写回主存
        // T2中维护的缓存以及主存中并没有集合C，造成数据不一致
        try {
            int i = 0;
            try {
                for (E e : c) {
                    checkNotNull(e);
                    items[i++] = e; // 添加进队列
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                throw new IllegalArgumentException();
            }
            count = i;
            // putIndex达到数组大小 ，从0开始
            putIndex = (i == capacity) ? 0 : i;
        } finally {
            lock.unlock();
        }
    }


    // 入队，super.add里面调用了offer方法
    public boolean add(E e) {
        return super.add(e);
    }

    // 队列未满时返回true，满时返回false 非阻塞
    public boolean offer(E e) {
        checkNotNull(e);
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            // 队列满时，不阻塞等待，直接返回false
            if (count == items.length) return false;
            else {
                enqueue(e); // 队列未满，直接插入
                return true;
            }
        } finally {
            lock.unlock();
        }
    }

    // 队列满时阻塞等待，直到有空位，可被中断返回
    public void put(E e) throws InterruptedException {
        checkNotNull(e);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly(); // 响应中断
        try {
            // 当队列满时，使非满等待队列休眠
            while (count == items.length) notFull.await();
            enqueue(e); // 此时队列非满，插入元素，同时在该方法里唤醒非空等待队列
        } finally {
            lock.unlock();
        }
    }


    // 非阻塞 队列为空，返回null
    public E poll() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return (count == 0) ? null : dequeue();
        } finally {
            lock.unlock();
        }
    }


    // 出队 如果队列为空 阻塞 可被中断返回
    public E take() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            // 队列为空，阻塞
            while (count == 0) notEmpty.await();
            return dequeue(); // 队列非空，删除元素，同时在唤醒非满等待队列
        } finally {
            lock.unlock();
        }
    }


    // 返回队首元素，不删除
    public E peek() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return itemAt(takeIndex); // null when queue is empty
        } finally {
            lock.unlock();
        }
    }

    class Itrs {

        void elementDequeued() {
        }
    }

    public Iterator<E> iterator() {
        return Collections.emptyIterator();
    }
}


