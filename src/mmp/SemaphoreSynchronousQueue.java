package mmp;


public class SemaphoreSynchronousQueue<E> {

    E item = null;
    Semaphore sync = new Semaphore(0);
    Semaphore put = new Semaphore(1);
    Semaphore take = new Semaphore(0);

    public E take() throws InterruptedException {
        take.acquire();
        E x = item;
        sync.release();
        put.release();
        return x;
    }

    public void put (E x) throws InterruptedException{
        put.acquire();
        item = x;
        take.release();
        sync.acquire();
    }
}
