package mmp;

import java.util.concurrent.atomic.AtomicInteger;

// CLH锁 基于链表 公平 自旋锁
// 线程只在本地变量上自旋，它不断轮询前驱的状态，如果发现前驱释放了锁就结束自旋
public class TicketLock {
    private AtomicInteger serviceNum = new AtomicInteger(); // 服务号
    private AtomicInteger ticketNum = new AtomicInteger(); // 排队号

    public int lock() {
        // 首先原子性地获得一个排队号
        int myTicketNum = ticketNum.getAndIncrement();

        // 只要当前服务号不是自己的就不断轮询
        while (serviceNum.get() != myTicketNum) {
        }

        return myTicketNum;
    }

    public void unlock(int myTicket) {
        // 只有当前线程拥有者才能释放锁
        int next = myTicket + 1;
        serviceNum.compareAndSet(myTicket, next);
    }
}
