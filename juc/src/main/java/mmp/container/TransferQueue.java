package mmp.container;

import java.util.concurrent.TimeUnit;

public interface TransferQueue<E> extends BlockingQueue<E> {

    /**
     * 立即转交一个元素给消费者，如果此时队列没有消费者，那就false
     */
    boolean tryTransfer(E e);

    /**
     * 转交一个元素给消费者，如果此时队列没有消费者，那就阻塞
     */
    void transfer(E e) throws InterruptedException;

    /**
     * 带超时的tryTransfer
     */
    boolean tryTransfer(E e, long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * 是否有消费者等待接收数据，瞬时状态
     */
    boolean hasWaitingConsumer();

    /**
     * 返回还有多少个等待的消费者，瞬时状态
     */
    int getWaitingConsumerCount();
}