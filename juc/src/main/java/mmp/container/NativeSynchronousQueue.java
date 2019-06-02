package mmp.container;

public class NativeSynchronousQueue<E> {

    boolean putting = false;
    E item = null;

    public synchronized E take() throws InterruptedException {
        while (item == null)
            wait();
        E e = item;
        item = null;
        notifyAll();
        return e;
    }

    public synchronized void put(E e) throws InterruptedException {
        if (e == null)
            return;
        // 多线程put
        while (putting)
            wait();
        putting = true;
        item = e;
        // put成功 唤醒其他
        notifyAll();
        // 已经有了
        while (item != null)
            wait();
        putting = false;
        notifyAll();
    }
}
