package mmp.test;

import mmp.lock.ReentrantReadWriteLock;
import java.lang.Thread;

import java.util.Random;

public class ReadWriteLockTest {

    public static void main(String[] args) {
        final Queue queue = new Queue();

        for (int i = 0; i < 3; i++) {
            new Thread(() -> {
                while (true) queue.get();
            }).start();
        }

        for (int i = 0; i < 3; i++) {
            new Thread(() -> {
                while (true) {
                    queue.put(new Random().nextInt(10000));
                }
            }).start();
        }
    }


    static class Queue {
        private Object data = null; // 共享数据，一个线程能写，多个线程同时读

        private ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

        public void get() {
            rwl.readLock().lock(); // 上读锁，其他线程只能读不能写
            System.out.println(Thread.currentThread().getName() + " be ready to read data!");
            try {
                Thread.sleep((long) (Math.random() * 1000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(Thread.currentThread().getName() + "have read data :" + data);
            rwl.readLock().unlock(); // 释放读锁
        }

        public void put(Object data) {

            rwl.writeLock().lock(); // 上写锁，不允许其他线程读也不允许写
            System.out.println(Thread.currentThread().getName() + " be ready to write data!");
            try {
                Thread.sleep((long) (Math.random() * 1000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            this.data = data;
            System.out.println(Thread.currentThread().getName() + " have write data: " + data);
            rwl.writeLock().unlock(); // 释放写锁
        }
    }


}
