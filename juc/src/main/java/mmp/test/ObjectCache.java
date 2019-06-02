package mmp.test;

import mmp.Semaphore;
import mmp.lock.Lock;
import mmp.lock.ReentrantLock;

/**
 * 基于信号量Semaphore的对象池实现
 */
public class ObjectCache<T> {

    public interface ObjectFactory<T> {

        T build();
    }

    /**
     * FIFO 单向链表
     */
    private class Node {

        T obj;

        Node next;
    }

    final int capacity;

    final ObjectFactory<T> factory;

    final Lock lock = new ReentrantLock();

    final Semaphore semaphore;

    private Node head;

    private Node tail;

    /**
     * 支持capacity个对象
     */
    public ObjectCache(int capacity, ObjectFactory<T> factory) {
        this.capacity = capacity;
        this.factory = factory;
        this.semaphore = new Semaphore(this.capacity);
        this.head = null;
        this.tail = null;
    }

    public T getObject() throws InterruptedException {
        // 如果对象的个数用完了，线程将被阻塞，直到有对象被返回
        semaphore.acquire();
        return getNextObject();
    }

    /**
     * 每次从头结点开始取对象
     * 如果头结点为空就构造一个新的对象返回
     * 否则将头结点对象取出，并且头结点后移
     */
    private T getNextObject() {
        // 信号量只是在信号不够的时候挂起线程，不保证信号量足够的时候获取对象和返还对象是线程安全的
        // 所以需要锁Lock来保证并发
        lock.lock();
        try {
            if (head == null) {
                return factory.build();
            } else {
                Node ret = head;
                head = head.next;
                if (head == null)
                    tail = null;
                ret.next = null;
                return ret.obj;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 将对象加入FIFO的尾节点，并释放一个空闲的信号量
     */
    public void returnObject(T t) {
        returnObjectToPool(t);
        semaphore.release();
    }

    private void returnObjectToPool(T t) {
        lock.lock();
        try {
            Node node = new Node();
            node.obj = t;
            if (tail == null) {
                head = tail = node;
            } else {
                tail.next = node;
                tail = node;
            }

        } finally {
            lock.unlock();
        }
    }

}