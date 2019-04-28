package mmp.container;

import mmp.lock.Condition;
import mmp.lock.ReentrantLock;

import java.util.Collection;

// Double Ended Queue
// 双向链表实现的双向并发阻塞队列
// 该阻塞队列同时支持FIFO和FILO两种操作方式，即可以从队列的头和尾同时操作(插入/删除)
// 使用一个可重入锁和这个锁生成的两个条件对象进行并发控制
public class LinkedBlockingDeque<E> {

    static final class Node<E> {

        E item;

        // 前驱节点
        Node<E> prev;

        // 后继节点
        Node<E> next;

        Node(E x) {
            item = x;
        }
    }

    // 双向链表的表头
    transient Node<E> first;

    // 双向链表的表尾
    transient Node<E> last;

    // 链表实际大小
    private transient int count;

    // 链表容量
    private final int capacity;


    final ReentrantLock lock = new ReentrantLock();


    private final Condition notEmpty = lock.newCondition();


    private final Condition notFull = lock.newCondition();


    public LinkedBlockingDeque() {
        this(Integer.MAX_VALUE);
    }

    public LinkedBlockingDeque(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException();
        this.capacity = capacity;
    }

    public LinkedBlockingDeque(Collection<? extends E> c) {
        this(Integer.MAX_VALUE);
        final ReentrantLock lock = this.lock;
        lock.lock(); // Never contended, but necessary for visibility
        try {
            for (E e : c) {
                if (e == null) throw new NullPointerException();
                if (!linkLast(new Node<>(e))) throw new IllegalStateException("Deque full");
            }
        } finally {
            lock.unlock();
        }
    }


    public boolean offerFirst(E e) {
        if (e == null) throw new NullPointerException();
        Node<E> node = new Node<>(e);
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return linkFirst(node);
        } finally {
            lock.unlock();
        }
    }

    public boolean offerLast(E e) {
        if (e == null) throw new NullPointerException();
        Node<E> node = new Node<>(e);
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return linkLast(node);
        } finally {
            lock.unlock();
        }
    }

    public void putFirst(E e) throws InterruptedException {
        if (e == null) throw new NullPointerException();
        Node<E> node = new Node<>(e);
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            while (!linkFirst(node)) notFull.await();
        } finally {
            lock.unlock();
        }
    }

    public void putLast(E e) throws InterruptedException {
        if (e == null) throw new NullPointerException();
        Node<E> node = new Node<>(e);
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            while (!linkLast(node)) notFull.await();
        } finally {
            lock.unlock();
        }
    }


    private boolean linkFirst(Node<E> node) {
        // 超过容量
        if (count >= capacity) return false;
        Node<E> f = first;
        node.next = f; // 新节点.next -> 原first
        first = node; // first -> 新节点
        // 没有尾节点，将新节点设置成尾节点
        if (last == null) last = node;
            // 有尾节点，原first.pre -> 新节点
        else f.prev = node;
        // 节点数量++
        ++count;
        // 新节点入队，通知非空条件队列
        notEmpty.signal();
        return true;
    }


    private boolean linkLast(Node<E> node) {
        if (count >= capacity) return false;
        Node<E> l = last;
        node.prev = l; // 新节点.prev -> 原last
        last = node; // last -> 新节点
        // 没有头节点，将新节点置成头节点
        if (first == null) first = node;
            // 有头节点，原last.next -> 新节点
        else l.next = node;
        ++count;
        notEmpty.signal();
        return true;
    }


    public E pollFirst() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return unlinkFirst();
        } finally {
            lock.unlock();
        }
    }

    public E pollLast() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return unlinkLast();
        } finally {
            lock.unlock();
        }
    }

    public E takeFirst() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            E x;
            while ((x = unlinkFirst()) == null) notEmpty.await();
            return x;
        } finally {
            lock.unlock();
        }
    }

    public E takeLast() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            E x;
            while ((x = unlinkLast()) == null) notEmpty.await();
            return x;
        } finally {
            lock.unlock();
        }
    }


    private E unlinkFirst() {

        Node<E> f = first;
        if (f == null) return null;
        Node<E> n = f.next;
        E item = f.item; // 原头节点内容
        f.item = null;
        f.next = f; // help GC
        first = n; // 头节点指向原第二个节点
        // 只有一个节点，移除头结点后，链表空，first -> null && last -> null
        if (n == null) last = null;
            // 否则，原第二个节点.pre -> null 现在是头节点了
        else n.prev = null;
        --count;
        notFull.signal(); // 通知非满条件队列
        return item;
    }


    private E unlinkLast() {
        Node<E> l = last;
        if (l == null) return null;
        Node<E> p = l.prev;
        E item = l.item;
        l.item = null;
        l.prev = l; // help GC
        last = p;
        if (p == null) first = null;
        else p.next = null;
        --count;
        notFull.signal();
        return item;
    }


    void unlink(Node<E> x) {
        // assert lock.isHeldByCurrentThread();
        Node<E> p = x.prev; // 前一个节点
        Node<E> n = x.next; // 后一个节点
        // prev==null 说明为头结点
        if (p == null) {
            unlinkFirst();
        }

        // next==null 说明为尾节点
        else if (n == null) {
            unlinkLast();
        }
        // 链表中间
        else {
            p.next = n;
            n.prev = p;
            x.item = null;
            // 没有断开原节点x链接，可能有其他线程在迭代链表
            --count;
            notFull.signal();
        }
    }

    public E peekFirst() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return (first == null) ? null : first.item;
        } finally {
            lock.unlock();
        }
    }

    // peekLast...

}
