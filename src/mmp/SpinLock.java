package mmp;

import java.util.concurrent.atomic.AtomicBoolean;

public class SpinLock {

    private AtomicBoolean mutex = new AtomicBoolean(false);

    public void lock() {
        while (!mutex.compareAndSet(false, true)) {
            // System.out.println(Thread.currentThread().getName()+ " wait lock release");
        }
    }

    public void unlock() {
        while (!mutex.compareAndSet(true, false)) {
            // System.out.println(Thread.currentThread().getName()+ " wait lock release");
        }
    }

}
