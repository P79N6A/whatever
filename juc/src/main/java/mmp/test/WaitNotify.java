package mmp.test;

import java.lang.Thread;

public class WaitNotify {

    final static Object_ lock = new Object_();

    public static void main(String[] args) {

        new Thread(() -> {
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (Exception e) {
                }
            }
        }, "线程 A").start();
        new Thread(() -> {
            synchronized (lock) {
                try {
                    lock.notify();
                } catch (Exception e) {
                }
            }
        }, "线程 B").start();

        // synchronized (lock) {
        //     while (flag) {
        //         lock.wait();
        //     }
        // }

        // notifyAll和notify不会立即生效，必须等到调用方执行完同步代码块，放弃锁之后才起作用
        // synchronized (lock) {
        //     flag
        //     lock.notifyAll();
        // }

    }

}