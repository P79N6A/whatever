package mmp.test;


import mmp.lock.LockSupport;

import java.lang.Thread;

public class ThreadTest {


    public static void main(String[] args) {
        Thread.currentThread().interrupt();
        LockSupport.park();
        System.out.println("不会阻塞");

    }


    private void f() throws Exception {
        // wait方法会释放锁，sleep方法不会释放锁
    }

    private void e() throws Exception {
        Thread.yield(); // yield让出CPU时间
    }


    private void d() throws Exception {

        Thread addThread = new Thread(() -> {
            for (; ; ) {
            }
        });

        addThread.start();

        // 将几个并发执行线程的线程合并为一个单线程执行
        addThread.join(); // 调用者线程阻塞挂起，直到addThread结束才被唤醒
        // join本质是调用了wait方法，让调用线程wait在当前线程对象实例上
        // addThread结束后，会调用notifyAll方法，注意不要再程序中调用线程的wait或者notify方法

    }

    private void c() throws Exception {
        Thread t1 = new Thread(() -> {
            for (; ; ) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    System.err.println("Interrupt When Sleep");
                    // 抛出InterruptedException之前，JVM 会先把该线程的中断标志位复位
                    // 在sleep中断线程导致抛出异常，会清清除中断位，需要重新设置中断位，下次循环则会直接判断中断标记，从而break
                    Thread.currentThread().interrupt();
                    // 该方法会清除中断状态，导致上面的一行代码失效
                    // boolean isInterrupt = Thread.interrupted();
                    // System.out.println(isInterrupt);
                }
                Thread.yield();
            }
        });

        t1.start();
        Thread.sleep(1000);
        t1.interrupt();

    }


    private void b() throws Exception {

        Thread t1 = new Thread(() -> {
            for (; ; ) {
                if (Thread.currentThread().isInterrupted()) break;
                Thread.yield();
            }
        });
        t1.start();
        Thread.sleep(2000);
        t1.interrupt();

    }

    private void a() throws Exception {
        Thread t1 = new Thread(() -> {
            for (; ; ) {
            }
        });
        t1.start();
        Thread.sleep(2000);
        // 无效，需要判断中断位状态
        t1.interrupt();
    }
}
