package mmp;


public class NativeSynchronousQueue<E> {

    boolean putting = false;
    E item = null;

    public synchronized E take() throws InterruptedException {
        while (item == null) wait();
        E e = item;
        item = null;
        notifyAll();
        return e;
    }

    public synchronized void put(E e) throws InterruptedException {
        if (e == null) return;
        while (putting) wait();
        putting = true;
        item = e;
        notifyAll();
        while (item != null) wait();
        putting = false;
        notifyAll();
    }
}
