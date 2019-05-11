package mmp.container;


import mmp.lock.Condition;
import mmp.lock.ReentrantLock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;


// 链表实现的、有界阻塞队列 FIFO 单向 写锁和读锁 可以并行执行放入数据和消费数据
public class LinkedBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {


    static class Node<E> {
        E item;

        /**
         * 当前节点的后一个节点，有三种情况：
         * - 真正的节点 the real successor Node
         * - 当前节点本身，说明当前节点是头结点 this Node, meaning the successor is head.next
         * - null，说明这个节点是最后的节点 null, meaning there is no successor (this is the last node)
         */
        Node<E> next;

        Node(E x) {
            item = x;
        }
    }


    // 当前容量，默认Integer.MAX_VALUE
    private final int capacity;

    // 队列中的元素数量
    private final AtomicInteger count = new AtomicInteger();

    // 队列头节点 头节点的item永远为null
    transient Node<E> head;

    // 队列尾节点，尾节点next永远为null
    private transient Node<E> last;

    // 获取元素的锁
    private final ReentrantLock takeLock = new ReentrantLock();
    // 等待取元素的等待队列
    private final Condition notEmpty = takeLock.newCondition();


    // 添加元素的锁
    private final ReentrantLock putLock = new ReentrantLock();
    // 等待添加元素的等待队列
    private final Condition notFull = putLock.newCondition();


    private void signalNotEmpty() {
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            notEmpty.signal();
        } finally {
            takeLock.unlock();
        }
    }


    private void signalNotFull() {
        final ReentrantLock putLock = this.putLock;
        putLock.lock();
        try {
            notFull.signal();
        } finally {
            putLock.unlock();
        }
    }

    // 入队
    private void enqueue(Node<E> node) {
        // <-
        last = last.next = node;
    }

    // 出队
    private E dequeue() {

        Node<E> h = head; // 头节点
        Node<E> first = h.next; // 实际上的第一个元素
        h.next = h; // 头节点的下一个节点指向自己 自引用 help GC
        head = first; // 头节点切到第一个元素
        E x = first.item; // 获取出队元素
        first.item = null; // 第一个元素（头节点置空）
        return x;
    }


    void fullyLock() {
        putLock.lock();
        takeLock.lock();
    }


    void fullyUnlock() {
        takeLock.unlock();
        putLock.unlock();
    }

    public LinkedBlockingQueue() {
        this(Integer.MAX_VALUE);
    }


    public LinkedBlockingQueue(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException();
        this.capacity = capacity;
        last = head = new Node<>(null); // 头指针和尾指针指向头节点（null）
    }


    public LinkedBlockingQueue(Collection<? extends E> c) {
        this(Integer.MAX_VALUE);
        final ReentrantLock putLock = this.putLock;
        putLock.lock(); // Never contended, but necessary for visibility
        try {
            int n = 0;
            for (E e : c) {
                // 拒绝空值
                if (e == null) throw new NullPointerException();
                if (n == capacity) throw new IllegalStateException("Queue full");
                enqueue(new Node<>(e));
                ++n;
            }
            count.set(n);
        } finally {
            putLock.unlock();
        }
    }


    public int size() {
        return count.get();
    }

    public boolean add(E e) {
        return true;
    }

    public boolean remove(Object o) {
        return true;
    }

    public int drainTo(Collection<? super E> c) {
        return 1;
    }

    public boolean isEmpty() {
        return true;
    }

    public E remove() {
        return null;
    }

    public Object[] toArray() {
        return null;
    }

    public void clear() {
    }


    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        return null;
    }

    public void put(E e) throws InterruptedException {
        if (e == null) throw new NullPointerException(); // 不允许空元素
        int c = -1;
        Node<E> node = new Node<>(e); // 以新元素构造节点
        final ReentrantLock putLock = this.putLock;
        final AtomicInteger count = this.count;
        putLock.lockInterruptibly();
        // putLock，保证调用put方法的时候只有1个线程
        try {
            // 如果队列已经满了，阻塞并挂起当前线程
            while (count.get() == capacity) notFull.await();
            enqueue(node); // 节点添加到链表尾部
            c = count.getAndIncrement();  // 元素个数+1
            // 再获取写队列中的元素，如果容量还没满，唤醒其他阻塞的添加线程
            if (c + 1 < capacity) notFull.signal();
        } finally {
            // 释放锁，让其他线程可以调用put方法
            putLock.unlock();
        }
        // 如果之前为 0，就通知等待的，队列里还有1条数据
        // 由于存在put锁和take锁，可能take锁一直在消费数据，count会变化
        if (c == 0) signalNotEmpty();
    }


    public boolean offer(E e) {
        if (e == null) throw new NullPointerException();
        final AtomicInteger count = this.count;
        if (count.get() == capacity) return false;
        int c = -1;
        Node<E> node = new Node<>(e);
        final ReentrantLock putLock = this.putLock;
        putLock.lock();
        try {
            if (count.get() < capacity) {
                enqueue(node);
                c = count.getAndIncrement();
                if (c + 1 < capacity) notFull.signal();
            }
        } finally {

            putLock.unlock();
        }
        if (c == 0) signalNotEmpty();
        return c >= 0; // 添加成功返回true，否则返回false
    }

    public E take() throws InterruptedException {
        E x;
        int c = -1;
        final AtomicInteger count = this.count;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lockInterruptibly(); // take锁加锁，保证调用take方法的时候只有1个线程
        try {
            // 如果队列里已经没有元素了 阻塞并挂起当前线程
            while (count.get() == 0) notEmpty.await();
            x = dequeue(); // 删除头结点
            c = count.getAndDecrement(); // 元素个数-1
            // 如果队列里还有元素
            if (c > 1) notEmpty.signal();
        } finally {
            takeLock.unlock();
        }
        if (c == capacity) signalNotFull();
        return x;
    }


    public E poll() {
        final AtomicInteger count = this.count;
        // 如果元素个数为0  返回null
        if (count.get() == 0) return null;
        E x = null;
        int c = -1;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            // 判断队列里是否还有数据
            if (count.get() > 0) {
                x = dequeue(); // 删除返回头结点
                c = count.getAndDecrement();
                // 如果队列里还有元素
                if (c > 1) notEmpty.signal();
            }
        } finally {
            takeLock.unlock();
        }
        if (c == capacity) signalNotFull();
        return x;
    }

    // 获取但是不移除当前队列的头元素，没有则返回null
    public E peek() {
        if (count.get() == 0) return null;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            Node<E> first = head.next;
            if (first == null) return null;
            else return first.item;
        } finally {
            takeLock.unlock();
        }
    }


    void unlink(Node<E> p, Node<E> trail) {
        // assert isFullyLocked();
        // p.next is not changed, to allow iterators that are
        // traversing p to maintain their weak-consistency guarantee.
        p.item = null;
        trail.next = p.next;
        if (last == p) last = trail;
        if (count.getAndDecrement() == capacity) notFull.signal();
    }


    public Iterator<E> iterator() {
        return new Itr();
    }

    private class Itr implements Iterator<E> {

        private Node<E> current;
        private Node<E> lastRet;
        private E currentElement;

        Itr() {
            fullyLock();
            try {
                current = head.next;
                if (current != null) currentElement = current.item;
            } finally {
                fullyUnlock();
            }
        }

        public boolean hasNext() {
            return current != null;
        }


        private Node<E> nextNode(Node<E> p) {
            for (; ; ) {
                Node<E> s = p.next;
                if (s == p) return head.next;
                if (s == null || s.item != null) return s;
                p = s;
            }
        }

        public E next() {
            fullyLock();
            try {
                if (current == null) throw new NoSuchElementException();
                E x = currentElement;
                lastRet = current;
                current = nextNode(current);
                currentElement = (current == null) ? null : current.item;
                return x;
            } finally {
                fullyUnlock();
            }
        }

        public void remove() {
            if (lastRet == null) throw new IllegalStateException();
            fullyLock();
            try {
                Node<E> node = lastRet;
                lastRet = null;
                for (Node<E> trail = head, p = trail.next; p != null; trail = p, p = p.next) {
                    if (p == node) {
                        unlink(p, trail);
                        break;
                    }
                }
            } finally {
                fullyUnlock();
            }
        }
    }


}
