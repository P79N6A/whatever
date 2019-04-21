package mmp.test;

public class Object_ {

    /*
     * 当线程试图访问synchronized代码块时，必须获得引用的对象的锁
     * 1、 假如这个锁已被其它线程占用，JVM会把此线程放到本对象的锁池中。本线程进入阻塞状态。
     * 等到其他的线程释放了锁，JVM就会从锁池中随机取出一个线程，使这个线程拥有锁，并且转到就绪状态。
     * 2、 假如这个锁没被其他线程占用，本线程会获得这把锁，开始执行同步代码块。
     * 如在执行同步代码块时，遇到异常而导致线程终止，锁会被释放；
     * 在执行代码块时，执行了锁所属对象的wait()方法，这个线程会释放对象锁，进入对象的等待池中
     * 线程开始执行同步代码块时，可以执行Thread.sleep()或Thread.yield()，此时它并不释放对象锁，只是把运行的机会让给其他的线程
     * synchronized不会被继承，如果一个synchronized方法被子类覆盖，子类中这个方法不同步，除非用synchronized修饰
     *
     * */

    /*
     * 为何 wait notify notifyAll 不是Thread类中的方法，而是Object类中的方法？
     * 每个对象都拥有monitor（即锁），所以让当前线程等待某个对象的锁，当然应该通过这个对象来操作了。
     * 而不是用当前线程来操作，因为当前线程可能会等待多个线程的锁，如果通过线程来操作，就非常复杂了。
     *
     * */
    public final native void wait_(long timeout) throws InterruptedException;

    public final void wait_(long timeout, int nanos) throws InterruptedException {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout value is negative");
        }

        if (nanos < 0 || nanos > 999999) {
            throw new IllegalArgumentException("nanosecond timeout value out of range");
        }

        if (nanos > 0) {
            timeout++;
        }

        wait_(timeout);
    }

    // 让出 CPU，释放锁，使该线程进入该对象监视器的等待池
    // 让当前线程交出此对象的监视器，然后进入等待状态
    // 当前线程必须拥有这个对象的监视器，必须在同步块或者同步方法中进行 synchronized
    public final void wait_() throws InterruptedException {
        wait_(0);
    }


    // 唤醒一个正在等待该对象监视器的线程
    // JVM从对象的等待池中随机选择一个线程，把它转到对象的锁池中
    // 当前线程也必须拥有这个对象的监视器，必须在同步块或者同步方法中进行 synchronized
    public final native void notify_();

    public final native void notifyAll_();
}
