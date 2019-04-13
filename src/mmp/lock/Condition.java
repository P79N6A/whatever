package mmp.lock;


import java.util.Date;
import java.util.concurrent.TimeUnit;

public interface Condition {

    // 在信号或被中断之前等待
    void await() throws InterruptedException;

    // 在信号之前等待
    void awaitUninterruptibly();

    long awaitNanos(long nanosTimeout) throws InterruptedException;

    // 在信号、被中断或指定时间之前等待
    boolean await(long time, TimeUnit unit) throws InterruptedException;

    // 在信号、被中断或到达指定最后期限之前等待
    boolean awaitUntil(Date deadline) throws InterruptedException;

    // 唤醒一个等待线程
    void signal();

    // 唤醒所有等待线程
    void signalAll();
}

