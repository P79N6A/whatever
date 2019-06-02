package mmp.container;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class ConcurrentLinkedQueue<E> extends AbstractQueue<E> {

    private static class Node<E> {
        volatile E item;
        volatile Node<E> next;

        Node(E item) {
            UNSAFE.putObject(this, itemOffset, item);
        }

        boolean casItem(E cmp, E val) {
            return UNSAFE.compareAndSwapObject(this, itemOffset, cmp, val);
        }

        void lazySetNext(Node<E> val) {
            UNSAFE.putOrderedObject(this, nextOffset, val);
        }

        boolean casNext(Node<E> cmp, Node<E> val) {
            return UNSAFE.compareAndSwapObject(this, nextOffset, cmp, val);
        }

        // Unsafe mechanics

        private static final sun.misc.Unsafe UNSAFE;
        private static final long itemOffset;
        private static final long nextOffset;

        static {
            try {
                UNSAFE = sun.misc.Unsafe.getUnsafe();
                Class<?> k = Node.class;
                itemOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("item"));
                nextOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("next"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }

    private transient volatile Node<E> head;

    private transient volatile Node<E> tail;

    public ConcurrentLinkedQueue() {
        // 头节点和尾节点初始化的时候指向一个空节点
        head = tail = new Node<>(null);
    }

    public ConcurrentLinkedQueue(Collection<? extends E> c) {
        Node<E> h = null, t = null;
        for (E e : c) {
            checkNotNull(e);
            Node<E> newNode = new Node<>(e);
            if (h == null)
                h = t = newNode;
            else {
                t.lazySetNext(newNode);
                t = newNode;
            }
        }
        if (h == null)
            h = t = new Node<>(null);
        head = h;
        tail = t;
    }

    @Override
    public boolean add(E e) {
        return offer(e);
    }

    final void updateHead(Node<E> h, Node<E> p) {
        if (h != p && casHead(h, p))
            h.lazySetNext(h);
    }

    /**
     * 获取当前节点的next元素，如果是自引入节点则返回真正头节点
     */
    final Node<E> succ(Node<E> p) {
        Node<E> next = p.next;
        return (p == next) ? head : next;
    }

    /**
     * 入队的操作都是由CAS算法完成
     * 首先要定位出尾节点，其次使用CAS算法将入队节点设置成尾节点的next节点
     * 如果将tail节点一直指向尾节点
     * 每次即tail->next = newNode;tail = newNode;
     * 但在并发下每次更新tail节点意味着每次都要CAS更新tail节点，这样入队效率必然降低
     * 所以tail节点并不总是指向队列尾节点的原因就是减少更新tail节点的次数，提高入队效率
     */
    @Override
    public boolean offer(E e) {
        // 不允许null入队
        checkNotNull(e);
        // 将入队元素构造为Node节点，内部调用unsafe.putObject
        final Node<E> newNode = new Node<>(e);
        // 从尾节点插入
        for (Node<E> t = tail, p = t; ; ) {
            Node<E> q = p.next;

            // 1
            // p.next=null 说明p是尾节点，插入
            if (q == null) {
                // 由于多线程调用offer，可能多线程 A B 同时CAS插入p.next
                // 线程B 再次进入，将新节点加到最后一个节点后面
                if (p.casNext(null, newNode)) {
                    // 线程A CAS插入成功，更新当前尾节点tail（将tail指向队列尾节点），此时p==t 不更新tail
                    // 线程B更新tail成功
                    if (p != t)
                        casTail(t, newNode); // 更新失败了也OK，表示有其他线程成功更新了tail节点
                    // 成功
                    return true;
                }
            }
            // 2 poll
            // 哨兵节点 next无效 可能返回t，或者head，那就从head开始遍历
            // !=不是原子操作 先取得t的值，再执行t = tail，并取得新的t的值，然后比较这两个值是否相等
            // 并发环境下，有可能在获得左边的t值后，右边的t值被其他线程修改，这样，t != t 就成立了
            // 表示在比较的过程中，tail被其他线程修改了 就用新的tail为链表的尾，也就是等式右边的t
            // 如果tail没有被修改，则返回head，从头部开始重新查找链表末尾。
            else if (p == q)
                p = (t != (t = tail)) ? t : head;

                // 3
                // 线程B CAS失败 再次循环进入这里 p==旧的tail q==新插入的node false 执行完后 p==q，然后再次进入循环，p.next==null 进入 1
            else
                p = (p != t && t != (t = tail)) ? t : q;

        }
    }

    @Override
    public E poll() {
        restartFromHead:
        for (; ; ) {
            for (Node<E> h = head, p = h, q; ; ) {
                // 保存当前节点值
                E item = p.item;
                // 当前节点有值则cas变为null（1）
                // 表头节点的数据不为null，并且设置表头数据为null的操作成功
                if (item != null && p.casItem(item, null)) {
                    //cas成功标志当前节点以及从链表中移除
                    // Successful CAS is the linearization point
                    // for item to be removed from this queue.
                    // hop two nodes at a time
                    // 类似tail间隔2设置一次头节点（2）
                    // 更新表头然后返回删除元素的值item
                    // 如果 p != h 为true，则更新表头然后返回删除元素的值item
                    if (p != h)
                        updateHead(h, ((q = p.next) != null) ? q : p);
                    // 如果 p != h 为true，则更新表头然后返回删除元素的值item
                    // 说明表头head也不是实时更新的，也是每两次更新一次
                    return item;
                }
                // 队列为空
                else if ((q = p.next) == null) {
                    updateHead(h, p);
                    // 没有其他线程添加元素
                    return null;
                }
                // 自引用了，则重新找新的队列头节点（4）
                else if (p == q)
                    continue restartFromHead;
                    // (5)
                else
                    p = q;

            }
        }
    }

    @Override
    public E peek() {
        restartFromHead:
        for (; ; ) {
            for (Node<E> h = head, p = h, q; ; ) {
                E item = p.item;
                if (item != null || (q = p.next) == null) {
                    updateHead(h, p);
                    return item;
                } else if (p == q)
                    continue restartFromHead;
                else
                    p = q;
            }
        }
    }

    /**
     * 获取第一个队列元素（哨兵元素不算），没有则为null
     */
    Node<E> first() {
        restartFromHead:
        for (; ; ) {
            for (Node<E> h = head, p = h, q; ; ) {
                boolean hasItem = (p.item != null);
                if (hasItem || (q = p.next) == null) {
                    updateHead(h, p);
                    return hasItem ? p : null;
                } else if (p == q)
                    continue restartFromHead;
                else
                    p = q;
            }
        }
    }

    @Override
    public boolean isEmpty() {
        return first() == null;
    }

    /**
     * 遍历整个队列 效率不高 没有加锁 可能不准
     * for (int i = 0, int size = concurrentLinkedQueue.size(); i < size;i++)
     * 不用每次循环都调用一次size方法遍历一遍队列
     */
    @Override
    public int size() {
        int count = 0;
        for (Node<E> p = first(); p != null; p = succ(p))
            if (p.item != null)
                if (++count == Integer.MAX_VALUE)
                    break;
        return count;
    }

    @Override
    public boolean remove(Object o) {
        if (o != null) {
            Node<E> next, pred = null;
            for (Node<E> p = first(); p != null; pred = p, p = next) {
                boolean removed = false;
                E item = p.item;
                if (item != null) {
                    if (!o.equals(item)) {
                        next = succ(p);
                        continue;
                    }
                    removed = p.casItem(item, null);
                }

                next = succ(p);
                // unlink
                if (pred != null && next != null)
                    pred.casNext(p, next);
                if (removed)
                    return true;
            }
        }
        return false;
    }

    @Override
    public Iterator<E> iterator() {
        return new Itr();
    }

    private class Itr implements Iterator<E> {

        private Node<E> nextNode;

        private E nextItem;

        private Node<E> lastRet;

        Itr() {
            advance();
        }

        /**
         * Moves to next valid node and returns item to return for
         * next(), or null if no such.
         */
        private E advance() {
            lastRet = nextNode;
            E x = nextItem;

            Node<E> pred, p;
            if (nextNode == null) {
                p = first();
                pred = null;
            } else {
                pred = nextNode;
                p = succ(nextNode);
            }

            for (; ; ) {
                if (p == null) {
                    nextNode = null;
                    nextItem = null;
                    return x;
                }
                E item = p.item;
                if (item != null) {
                    nextNode = p;
                    nextItem = item;
                    return x;
                } else {
                    // skip over nulls
                    Node<E> next = succ(p);
                    if (pred != null && next != null)
                        pred.casNext(p, next);
                    p = next;
                }
            }
        }

        @Override
        public boolean hasNext() {
            return nextNode != null;
        }

        @Override
        public E next() {
            if (nextNode == null)
                throw new NoSuchElementException();
            return advance();
        }

        @Override
        public void remove() {
            Node<E> l = lastRet;
            if (l == null)
                throw new IllegalStateException();
            // rely on a future traversal to relink.
            l.item = null;
            lastRet = null;
        }
    }

    private static void checkNotNull(Object v) {
        if (v == null)
            throw new NullPointerException();
    }

    private boolean casTail(Node<E> cmp, Node<E> val) {
        return UNSAFE.compareAndSwapObject(this, tailOffset, cmp, val);
    }

    private boolean casHead(Node<E> cmp, Node<E> val) {
        return UNSAFE.compareAndSwapObject(this, headOffset, cmp, val);
    }

    // Unsafe mechanics

    private static final sun.misc.Unsafe UNSAFE;
    private static final long headOffset;
    private static final long tailOffset;

    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> k = ConcurrentLinkedQueue.class;
            headOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("head"));
            tailOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("tail"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}


