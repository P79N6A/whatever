package mmp;


import java.util.Collection;
import java.util.concurrent.TimeUnit;


/*
 * 队列操作当前不可用时，四种处理方式：
 * 抛出异常： add(), remove(), element()
 * 返回某个值（null 或 false）：offer(), poll(), peek()
 * 阻塞当前线程，直到操作可以进行：put(), take()
 * 阻塞一段时间，超时后退出：offer, poll()
 * BlockingQueue 不允许有 null 元素，null 是异常返回值
 * */
public interface BlockingQueue<E> extends Queue<E> {
    // 添加失败时会抛出异常
    boolean add(E e);

    // 添加失败时会返回 false
    boolean offer(E e);

    // 添加元素时，如果没有空间，会阻塞等待；可以响应中断
    void put(E e) throws InterruptedException;

    // 添加元素到队列中，如果没有空间会等待参数中的时间，超时返回，会响应中断
    // boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException;

    // 获取并移除队首元素，如果没有元素就会阻塞等待
    E take() throws InterruptedException;

    // 获取并移除队首元素，如果没有就会阻塞等待参数的时间，超时返回
    E poll(long timeout, TimeUnit unit) throws InterruptedException;

    // 返回队列中剩余的空间
    // int remainingCapacity();

    // 移除队列中某个元素，如果存在的话返回 true，否则返回 false
    boolean remove(Object o);

    // 检查队列中是否包含某个元素，至少包含一个就返回 true
    // public boolean contains(Object o);

    // 将当前队列所有元素移动到给定的集合中，返回移动的元素个数
    int drainTo(Collection<? super E> c);

    // 移动队列中至多 maxElements 个元素到指定的集合中
    // int drainTo(Collection<? super E> c, int maxElements);
}
