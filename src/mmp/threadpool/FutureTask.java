package mmp.threadpool;

import mmp.lock.LockSupport;

import java.lang.Thread;

import java.util.concurrent.*;

public class FutureTask<V> implements RunnableFuture<V> {

    private volatile int state;
    private static final int NEW = 0; // 刚创建
    private static final int COMPLETING = 1; // 任务完成中
    private static final int NORMAL = 2; // 任务彻底完成
    private static final int EXCEPTIONAL = 3;
    private static final int CANCELLED = 4;
    private static final int INTERRUPTING = 5;
    private static final int INTERRUPTED = 6;

    private Callable<V> callable;

    private Object outcome; // non-volatile, protected by state reads/writes

    private volatile Thread runner;

    private volatile WaitNode waiters;

    // 拿到结果，判断状态，如果状态正常，就返回值，如果不正常，就抛出异常
    @SuppressWarnings("unchecked")
    private V report(int s) throws ExecutionException {
        Object x = outcome;
        if (s == NORMAL) return (V) x;
        if (s >= CANCELLED) throw new CancellationException();
        throw new ExecutionException((Throwable) x);
    }

    public FutureTask(Callable<V> callable) {
        if (callable == null) throw new NullPointerException();
        this.callable = callable;
        this.state = NEW;       // ensure visibility of callable
    }

    public FutureTask(Runnable runnable, V result) {
        // 适配器，将Runnable包装成 Callable
        this.callable = Executors.callable(runnable, result);
        this.state = NEW;       // ensure visibility of callable
    }

    public boolean isCancelled() {
        return state >= CANCELLED;
    }

    public boolean isDone() {
        return state != NEW;
    }

    // 取消任务
    public boolean cancel(boolean mayInterruptIfRunning) {
        // 如果任务已完成，或已取消，或者无法取消，则此尝试将失败
        if (!(state == NEW && UNSAFE.compareAndSwapInt(this, stateOffset, NEW, mayInterruptIfRunning ? INTERRUPTING : CANCELLED)))
            return false;

        // 如果调用成功，而此任务尚未启动，则此任务将永不运行
        try {    // in case call to interrupt throws exception
            // 如果任务已经启动，mayInterruptIfRunning参数确定是否中断执行此任务的线程
            if (mayInterruptIfRunning) {
                try {
                    Thread t = runner;
                    // 调用线程的interrupt方法
                    if (t != null) t.interrupt();
                } finally { // final state
                    // 将状态改成INTERRUPTED
                    UNSAFE.putOrderedInt(this, stateOffset, INTERRUPTED);
                }
            }
        } finally {
            finishCompletion();
        }
        return true;
    }

    // 挂起自己等待异步线程唤醒，然后拿去异步线程设置好的数据
    // 首先判断状态，然后挂起自己等待，最后返回结果
    // 并发访问get的时候，需要将这些线程保存在FutureTask内部的栈中
    public V get() throws InterruptedException, ExecutionException {
        int s = state;
        if (s <= COMPLETING) s = awaitDone(false, 0L);
        return report(s);
    }

    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (unit == null) throw new NullPointerException();
        int s = state;
        if (s <= COMPLETING && (s = awaitDone(true, unit.toNanos(timeout))) <= COMPLETING) throw new TimeoutException();
        return report(s);
    }

    protected void done() {
    }

    protected void set(V v) {
        // 先将状态变成COMPLETING
        if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) {
            // 然后设置结果
            outcome = v;
            // 再然后设置状态为NORMAL
            UNSAFE.putOrderedInt(this, stateOffset, NORMAL); // final state
            // 最后唤醒等待线程
            finishCompletion();
        }
    }

    protected void setException(Throwable t) {
        if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) {
            outcome = t;
            UNSAFE.putOrderedInt(this, stateOffset, EXCEPTIONAL); // final state
            finishCompletion();
        }
    }


    // 异步线程执行
    public void run() {
        // 判断状态
        if (state != NEW || !UNSAFE.compareAndSwapObject(this, runnerOffset, null, Thread.currentThread())) return;
        try {
            Callable<V> c = callable;
            if (c != null && state == NEW) {
                V result;
                boolean ran;
                try {
                    // 执行callable的call方法
                    result = c.call();
                    ran = true;
                } catch (Throwable ex) {
                    result = null;
                    ran = false;
                    setException(ex);
                }
                // 设置结果并唤醒等待的所有线程
                if (ran) set(result);
            }
        } finally {
            // runner must be non-null until state is settled to
            // prevent concurrent calls to run()
            runner = null;
            // state must be re-read after nulling runner to prevent
            // leaked interrupts
            int s = state;
            // 在INTERRUPTING和INTERRUPTED的这段时间，需要自旋等待状态变成INTERRUPTED
            if (s >= INTERRUPTING) handlePossibleCancellationInterrupt(s);
        }
    }

    protected boolean runAndReset() {
        if (state != NEW || !UNSAFE.compareAndSwapObject(this, runnerOffset, null, Thread.currentThread()))
            return false;
        boolean ran = false;
        int s = state;
        try {
            Callable<V> c = callable;
            if (c != null && s == NEW) {
                try {
                    c.call(); // don't set result
                    ran = true;
                } catch (Throwable ex) {
                    setException(ex);
                }
            }
        } finally {
            // runner must be non-null until state is settled to
            // prevent concurrent calls to run()
            runner = null;
            // state must be re-read after nulling runner to prevent
            // leaked interrupts
            s = state;
            if (s >= INTERRUPTING) handlePossibleCancellationInterrupt(s);
        }
        return ran && s == NEW;
    }

    private void handlePossibleCancellationInterrupt(int s) {
        // It is possible for our interrupter to stall before getting a
        // chance to interrupt us.  Let's spin-wait patiently.
        if (s == INTERRUPTING) while (state == INTERRUPTING) Thread.yield(); // wait out pending interrupt
    }


    // 栈结构，后进先出(LIFO)
    static final class WaitNode {
        volatile Thread thread;
        volatile WaitNode next;

        WaitNode() {
            thread = Thread.currentThread();
        }
    }

    private void finishCompletion() {
        // assert state > COMPLETING;
        for (WaitNode q; (q = waiters) != null; ) {
            // 先将waiters修改成null
            if (UNSAFE.compareAndSwapObject(this, waitersOffset, q, null)) {
                // 遍历栈中所有节点，也就是所有等待的线程，依次唤醒他们
                for (; ; ) {
                    Thread t = q.thread;
                    if (t != null) {
                        q.thread = null;
                        LockSupport.unpark(t);
                    }
                    WaitNode next = q.next;
                    if (next == null) break;
                    q.next = null; // unlink to help gc
                    q = next;
                }
                break;
            }
        }

        // 子类扩展方法
        done();

        callable = null;        // to reduce footprint
    }

    // 挂起
    private int awaitDone(boolean timed, long nanos) throws InterruptedException {
        final long deadline = timed ? System.nanoTime() + nanos : 0L;
        WaitNode q = null;
        boolean queued = false;
        for (; ; ) {
            // 如果线程中断了，删除节点，并抛出异常
            if (Thread.interrupted()) {
                removeWaiter(q);
                throw new InterruptedException();
            }

            int s = state;
            // 大于COMPLETING，说明任务完成了，返回结果
            if (s > COMPLETING) {
                if (q != null) q.thread = null;
                return s;
            }
            // 如果等于COMPLETING，说明任务快要完成了，自旋一会
            else if (s == COMPLETING) Thread.yield();
                // 如果q是null，说明这是第一次进入，创建一个新的节点并保存当前线程引用
            else if (q == null) q = new WaitNode();
                // 如果还没有修改过waiters变量，CAS修改当前waiters为当前节点，这里是栈结构
            else if (!queued) queued = UNSAFE.compareAndSwapObject(this, waitersOffset, q.next = waiters, q);
                // 根据时间策略挂起当前线程
            else if (timed) {
                nanos = deadline - System.nanoTime();
                if (nanos <= 0L) {
                    removeWaiter(q);
                    return state;
                }
                LockSupport.parkNanos(this, nanos);
            } else LockSupport.park(this);
            // 线程醒来后，继续上面的判断，返回数据
        }
    }

    private void removeWaiter(WaitNode node) {
        if (node != null) {
            node.thread = null;
            retry:
            for (; ; ) {          // restart on removeWaiter race
                for (WaitNode pred = null, q = waiters, s; q != null; q = s) {
                    s = q.next;
                    if (q.thread != null) pred = q;
                    else if (pred != null) {
                        pred.next = s;
                        if (pred.thread == null) continue retry;
                    } else if (!UNSAFE.compareAndSwapObject(this, waitersOffset, q, s)) continue retry;
                }
                break;
            }
        }
    }

    // Unsafe mechanics
    private static final sun.misc.Unsafe UNSAFE;
    private static final long stateOffset;
    private static final long runnerOffset;
    private static final long waitersOffset;

    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> k = FutureTask.class;
            stateOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("state"));
            runnerOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("runner"));
            waitersOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("waiters"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

}
