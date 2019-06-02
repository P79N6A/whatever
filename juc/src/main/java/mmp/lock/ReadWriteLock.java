package mmp.lock;

public interface ReadWriteLock {

    /**
     * 读取锁用于读操作，共享锁，能同时被多个线程获取
     */
    Lock readLock();

    /**
     * 写入锁用于写操作，独占锁，只能被一个线程锁获取
     */
    Lock writeLock();

    /*
     * 不能同时存在读取锁和写入锁
     * 可以读/读
     * 不能读/写、写/写
     */

}




