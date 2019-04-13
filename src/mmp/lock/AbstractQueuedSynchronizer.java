package mmp.lock;

import sun.misc.Unsafe;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractOwnableSynchronizer;

public abstract class AbstractQueuedSynchronizer extends AbstractOwnableSynchronizer {

    protected AbstractQueuedSynchronizer() {
    }

    // AQS队列的节点，双向链表
    static final class Node {
        // 共享模式
        static final Node SHARED = new Node();
        // 独占模式
        static final Node EXCLUSIVE = null;
        // 因为超时或中断，线程已被取消，等待被踢出队列
        static final int CANCELLED = 1;
        // 当前节点的的后继节点将要或已被阻塞，在当前节点释放的时要unpark后继节点
        static final int SIGNAL = -1;
        // 线程在Condition队列等待唤醒
        static final int CONDITION = -2;
        // releaseShared需要被传播给后续节点，后续的acquireShared可以执行
        // 共享模式头Node，锁的下一次获取可以无条件传播
        static final int PROPAGATE = -3;

        // 节点的状态 CANCELLED, SIGNAL, CONDITION, PROPAGATE, 0(节点在队列中等待获取锁)
        volatile int waitStatus;

        // AQS队列前驱节点
        volatile Node prev;

        // AQS队列后继节点
        volatile Node next;

        // 节点对应的线程
        volatile Thread thread;


        // Condition队列中的后继节点，并区别独占锁还是共享锁
        Node nextWaiter;

        // 共享锁或独占锁
        final boolean isShared() {
            return nextWaiter == SHARED;
        }

        // 返回前一节点
        final Node predecessor() throws NullPointerException {
            Node p = prev;
            if (p == null) throw new NullPointerException();
            else return p;
        }

        Node() {    // Used to establish initial head or SHARED marker
        }

        Node(Thread thread, Node mode) { // Used by addWaiter
            this.nextWaiter = mode;
            this.thread = thread;
        }

        Node(Thread thread, int waitStatus) { // Used by Condition
            this.waitStatus = waitStatus;
            this.thread = thread;
        }
    }

    // AQS队列的队首 头结点不存储Thread，仅保存next结点的引用
    private transient volatile Node head;

    // AQS队列的队尾
    private transient volatile Node tail;

    // 锁的状态
    // 对于独占锁，state=0表示锁是可获取状态(锁没有被任何线程锁持有)
    // 由于锁可重入，state的值可以>1
    private volatile int state;


    protected final int getState() {
        return state;
    }


    protected final void setState(int newState) {
        state = newState;
    }


    protected final boolean compareAndSetState(int expect, int update) {
        // See below for intrinsics setup to support this
        return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
    }

    // Queuing utilities

    final boolean apparentlyFirstQueuedIsExclusive() {
        Node h, s;
        return (h = head) != null && (s = h.next) != null && !s.isShared() && s.thread != null;
    }

    // 线程自旋等待的时间
    static final long spinForTimeoutThreshold = 1000L;


    // 死循环，直到成功加到同步队列尾部才退出，返回前继节点（原tail）
    private Node enq(final Node node) {
        // 自旋
        for (; ; ) {
            Node t = tail;
            // tail为null，说明还没初始化，创建一个空节点，CAS设为head，成功就将tail也指向它
            if (t == null) { // Must initialize
                if (compareAndSetHead(new Node())) tail = head;
            }
            // 否则，将当前线程节点作为tail节点加入到AQS中去
            else {
                node.prev = t;
                if (compareAndSetTail(t, node)) {
                    // CAS成功后将双向链表连起来
                    t.next = node;
                    return t;
                }
            }
        }
    }


    /*
     * 关于虚拟的head节点，作用是防止重复释放锁
     * 当第一个进入队列的节点没有前驱节点的时候，就会创建一个虚拟的，再把自己挂到末尾
     * 每个节点在休眠前，都要将前驱节点的 ws 设置成 SIGNAL，表示，当节点释放锁的时候，需要唤醒下一个节点，否则自己永远无法被唤醒
     * 但是第一个节点，没有前置节点，那就创建一个空节点，保持一致
     * */


    // 将当前线程添加到AQS队尾，等待获取锁
    private Node addWaiter(Node mode) {
        // 将线程构造成节点
        Node node = new Node(Thread.currentThread(), mode);
        Node pred = tail;
        // 队列不为空，尝试直接挂到AQS同步队列队尾
        if (pred != null) {
            node.prev = pred;
            if (compareAndSetTail(pred, node)) {
                pred.next = node;
                return node;
            }
        }
        // 如果此时有多个线程都把自己挂到队尾，CAS就会失败，enq循环至成功
        enq(node);
        return node;
    }


    private void setHead(Node node) {
        head = node;
        // GC
        node.thread = null; // 将头结点的线程清空
        node.prev = null; // 将头结点的前任节点清空
    }


    // 一旦头节点的后继节点被唤醒，后继节点就会尝试去获取锁
    // 在acquireQueued中node就是有效的后继节点，p就是唤醒它的头结点
    // 如果成功就会将头结点设置为自身，并且将头结点的前驱节点置空，这样前驱节点就可以被GC
    private void unparkSuccessor(Node node) {
        // 此时节点是head，代表要释放锁的节点
        int ws = node.waitStatus;
        // ws != CANCELLED
        // CAS将head.ws置为0，清除信号，表示已释放，不能重复释放
        if (ws < 0) compareAndSetWaitStatus(node, ws, 0);

        // 从头结点的下一个节点开始寻找后继节点
        Node s = node.next;
        // 当且仅当后继节点的waitStatus<=0才是有效后继节点
        // 否则将waitStatus>0（CANCELLED）节点从队列中剔除
        // 通常这个节点是head.next，但如果head.next==null || head.next被取消了，就会从尾部开始找
        if (s == null || s.waitStatus > 0) {
            s = null;
            // 从尾部开始，向前寻找未被取消的节点，直到head
            for (Node t = tail; t != null && t != node; t = t.prev)
                if (t.waitStatus <= 0) s = t;
        }

        // 如果找到一个有效的后继节点，就唤醒此节点
        if (s != null) LockSupport.unpark(s.thread);

        // 关于从尾部查找的逻辑：
        // lock()的线程可能会被中断，这时已经进入CHL队列的节点就会被CANCELLED，也就是会移出队列
        // 移出队列 cancelAcquire 就是让前驱有效节点的next指向被移出节点node的next
        // cas(cancelled_node.pre.next, cancelled_node, cancelled_node.next);
        // 并且将cancelled_node.next指向cancelled_node，也就是cancelled_node没有后继节点了，但是不修改前驱节点
        // 如果从后往前遍历到被删出节点node时，根据cancelled_node.pre可以继续往前，直到head为止
        // 如果要想从head往后遍历，逻辑就是：cas(cancelled_node.next.pre, cancelled_node, cancelled_node.pre);
        // 差别在于，由于存在傀儡节点（head），cancelled_node.pre总是存在的，更容易处理

        // 唤醒后的逻辑：拿锁，设置自己为head，断开前任head和自己的连接

    }

    // 释放共享锁
    // 从前往后遍历AQS队列，依次唤醒
    private void doReleaseShared() {

        for (; ; ) {
            // 获取AQS队列的头节点
            Node h = head;
            // 队列中 head 节点不是 null，且和 tail 不相等
            if (h != null && h != tail) {
                int ws = h.waitStatus;
                // 如果头节点是SIGNAL状态，意味着头节点的后继节点需要被唤醒
                if (ws == Node.SIGNAL) {
                    // CAS 将头节点状态修改成 0
                    if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0)) continue;
                    // 如果成功，唤醒头节点的后继节点
                    unparkSuccessor(h);

                }
                // 成功设置成 0 之后，将 head 状态设置成传播状态
                else if (ws == 0 && !compareAndSetWaitStatus(h, 0, Node.PROPAGATE)) continue;
            }
            // 如果头节点没有变化，退出循环
            if (h == head) break;

        }
    }


    private void setHeadAndPropagate(Node node, int propagate) {
        Node h = head; // Record old head for check below
        setHead(node);
        if (propagate > 0 || h == null || h.waitStatus < 0 || (h = head) == null || h.waitStatus < 0) {
            Node s = node.next;
            if (s == null || s.isShared()) doReleaseShared();
        }
    }

    // Utilities for various versions of acquire

    private void cancelAcquire(Node node) {
        if (node == null) return;
        node.thread = null;
        Node pred = node.prev;
        while (pred.waitStatus > 0) node.prev = pred = pred.prev;
        Node predNext = pred.next;
        node.waitStatus = Node.CANCELLED;
        if (node == tail && compareAndSetTail(node, pred)) {
            compareAndSetNext(pred, predNext, null);
        } else {
            int ws;
            if (pred != head && ((ws = pred.waitStatus) == Node.SIGNAL ||
                    (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL))) && pred.thread != null) {
                Node next = node.next;
                if (next != null && next.waitStatus <= 0) compareAndSetNext(pred, predNext, next);
            } else {
                unparkSuccessor(node);
            }
            node.next = node; // help GC
        }
    }

    // 返回当前线程是否应该阻塞
    // 挂起自己之前，需要将前驱节点的ws设成SIGNAL，让前驱释放锁的时候唤醒自己
    private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
        int ws = pred.waitStatus;
        // 如果前驱节点已经是SIGNAL，当前线程可以安全被阻塞，返回true
        if (ws == Node.SIGNAL) return true;
        // 如果前驱节点非有效（CANCELLED），跳过并重试
        if (ws > 0) {
            do {
                // 将pred.prev赋值给node.prev
                node.prev = pred = pred.prev; // 前驱节点已取消，不会再获取同步状态，把前驱节点移除
            } while (pred.waitStatus > 0);
            // 将pred.prev的 next 赋值为当前节点
            pred.next = node;
        } else {
            // 如果前驱节点 0 || CONDITION || PROPAGATE，则设置前驱节点为SIGNAL
            compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
        }
        // false 重来
        return false;
    }

    // 当前线程自己产生一个中断
    // acquireQueued()中，当前线程被中断过，则执行selfInterrupt()，否则不会执行
    // acquireQueued()中，即使线程阻塞被中断唤醒而获取到CPU执行权利；
    // 但如果该线程的前面还有其它等待锁的线程，根据公平原则，该线程依然无法获取到锁，该线程再次阻塞
    // 直到该线程被它的前面等待锁的线程锁唤醒，线程才会获取锁，然后真正执行
    // 在该线程成功获取锁并真正执行之前，它的中断会被忽略并且中断标记会被清除
    // 因为在parkAndCheckInterrupt()中，线程的中断状态时调用了Thread.interrupted()
    // isInterrupted()仅仅返回中断状态，而interrupted()在返回当前中断状态之后，还会清除中断状态
    // 之前的中断状态被清除了，所以需要调用selfInterrupt()重新产生一个中断
    static void selfInterrupt() {
        // 如果线程曾中断或阻塞过，如手动interrupt()或超时，那就再中断一次
        Thread.currentThread().interrupt();
    }


    // 阻塞当前线程，并返回线程被唤醒之后的中断状态
    // 线程被阻塞后唤醒一般有两种情况：
    // 1：unpark。前驱节点用完锁之后，通过unpark()唤醒当前线程
    // 2：中断。其它线程通过interrupt()中断当前线程
    private final boolean parkAndCheckInterrupt() {
        LockSupport.park(this);
        return Thread.interrupted(); // 这里会复位
    }


    // 从队列中获取锁，并返回当前线程在等待过程中有没有中断过
    // 如果获取到了锁，返回，否则，公平阻塞等待，直到唤醒并重新获取锁了才返回
    final boolean acquireQueued(final Node node, int arg) {
        boolean failed = true;
        try {
            // interrupted表示在AQS队列的调度中，线程阻塞时，有没有被中断过
            boolean interrupted = false;
            // 自旋
            for (; ; ) {
                // 如果当前节点因为其它节点调用unpark()而被唤醒，那么唤醒它的应该是它的前驱节点
                final Node p = node.predecessor();

                // 公平
                // 如果节点的前驱是head（空节点），就可以尝试获取锁tryAcquire()
                if (p == head && tryAcquire(arg)) {
                    // 如果获取到，则将自己设置为头节点
                    setHead(node);
                    // 原head指向自己的next设成null
                    p.next = null; // help GC
                    failed = false;
                    // 返回false，没有中断
                    return interrupted;
                }

                // 如果当前节点是因为中断而唤醒，那就不公平了

                // 如果可以安全阻塞（设置好前继SIGNAL了），就尝试阻塞自己等待，然后返回线程的中断状态并复位中断状态

                // 如果当前线程是非中断状态，park时被阻塞，此时返回中断状态是false
                // 如果当前线程是中断状态，park不起作用，会立即返回，parkAndCheckInterrupt返回true，并复位
                // 再次循环进来时，由于之前已经复位该线程的中断状态，再次调用park方法时会阻塞
                if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt()) interrupted = true;
                // 这里判断中断是为了不让循环一直执行，让当前线程进入阻塞
                // 否则前一个线程在获取锁之后执行了耗时操作，那就要一直循环下去，造成CPU使用率飙升
            }
        } finally {
            if (failed) cancelAcquire(node);
        }
    }


    private void doAcquireInterruptibly(int arg) throws InterruptedException {
        final Node node = addWaiter(Node.EXCLUSIVE);
        boolean failed = true;
        try {
            for (; ; ) {
                final Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null; // help GC
                    failed = false;
                    return;
                }
                if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt()) throw new InterruptedException();
            }
        } finally {
            if (failed) cancelAcquire(node);
        }
    }


    private boolean doAcquireNanos(int arg, long nanosTimeout) throws InterruptedException {
        if (nanosTimeout <= 0L) return false;
        final long deadline = System.nanoTime() + nanosTimeout;
        final Node node = addWaiter(Node.EXCLUSIVE);
        boolean failed = true;
        try {
            for (; ; ) {
                final Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null; // help GC
                    failed = false;
                    return true;
                }
                nanosTimeout = deadline - System.nanoTime();
                if (nanosTimeout <= 0L) return false;
                if (shouldParkAfterFailedAcquire(p, node) && nanosTimeout > spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if (Thread.interrupted()) throw new InterruptedException();
            }
        } finally {
            if (failed) cancelAcquire(node);
        }
    }

    // 获取共享锁
    private void doAcquireShared(int arg) {
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            boolean interrupted = false;
            for (; ; ) {
                final Node p = node.predecessor();
                if (p == head) {
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        if (interrupted) selfInterrupt();
                        failed = false;
                        return;
                    }
                }
                // 如果当前线程不是AQS队列的表头，shouldParkAfterFailedAcquire()判断是否需要等待
                // 需要的话，则通过parkAndCheckInterrupt()进行阻塞等待
                // 若阻塞等待过程中，线程被中断过，则设置interrupted为true
                if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt()) interrupted = true;
            }
        } finally {
            if (failed) cancelAcquire(node);
        }
    }


    private void doAcquireSharedInterruptibly(int arg) throws InterruptedException {
        // 创建节点并添加到AQS队列末尾
        final Node node = addWaiter(Node.SHARED);
        // 中断失败标记
        boolean failed = true;
        try {
            for (; ; ) {
                final Node p = node.predecessor();
                // 如果前继节点是AQS队列的 head，说明前面已经没有线程阻挡它获取锁了，尝试获取共享锁
                if (p == head) {
                    // 获取锁的状态
                    int r = tryAcquireShared(arg);
                    // 说明可以获取锁
                    if (r >= 0) {
                        // 将当前线程设置为 head ，唤醒后面的线程，传播
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        // 没有发生错误，不必执行下面的取消操作
                        failed = false;
                        return;
                    }
                }
                // (前继节点不是AQS队列的表头) 等待直到获取到共享锁
                // 如果线程在等待过程中被中断过，则再次中断该线程(还原之前的中断状态)，抛出异常
                if (shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt()) throw new InterruptedException();
            }
        } finally {
            // 如果发生了中断异常，则取消获取锁
            if (failed) cancelAcquire(node);
        }
    }

    private boolean doAcquireSharedNanos(int arg, long nanosTimeout) throws InterruptedException {
        if (nanosTimeout <= 0L) return false;
        final long deadline = System.nanoTime() + nanosTimeout;
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            for (; ; ) {
                final Node p = node.predecessor();
                if (p == head) {
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        failed = false;
                        return true;
                    }
                }
                nanosTimeout = deadline - System.nanoTime();
                if (nanosTimeout <= 0L) return false;
                if (shouldParkAfterFailedAcquire(p, node) && nanosTimeout > spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);
                if (Thread.interrupted()) throw new InterruptedException();
            }
        } finally {
            if (failed) cancelAcquire(node);
        }
    }

    // Main exported methods


    protected boolean tryAcquire(int arg) {
        throw new UnsupportedOperationException();
    }

    // state-arg
    protected boolean tryRelease(int arg) {
        throw new UnsupportedOperationException();
    }


    protected int tryAcquireShared(int arg) {
        throw new UnsupportedOperationException();
    }


    protected boolean tryReleaseShared(int arg) {
        throw new UnsupportedOperationException();
    }

    protected boolean isHeldExclusively() {
        throw new UnsupportedOperationException();
    }


    // 获取独占锁，如果获取不到，加入等待队列等待被唤醒，如果被中断则中止
    public final void acquire(int arg) {
        // 首先通过tryAcquire()尝试获取锁，获取成功直接返回
        // 失败，addWaiter()将当前线程加入到AQS队列末尾
        // 然后调用acquireQueued()，在AQS队列中等待获取锁，在此过程中，线程阻塞，直到获取锁了才返回
        // 如果在等待过程中被中断过，则selfInterrupt()自己产生一个中断（之前的被清除了）
        if (!tryAcquire(arg) && acquireQueued(addWaiter(Node.EXCLUSIVE), arg)) selfInterrupt();
    }


    public final void acquireInterruptibly(int arg) throws InterruptedException {
        // 中断则抛出中断异常，并停止阻塞
        if (Thread.interrupted()) throw new InterruptedException();
        // 首先获取锁，参照上文
        if (!tryAcquire(arg)) doAcquireInterruptibly(arg);
    }


    public final boolean tryAcquireNanos(int arg, long nanosTimeout) throws InterruptedException {
        if (Thread.interrupted()) throw new InterruptedException();
        return tryAcquire(arg) || doAcquireNanos(arg, nanosTimeout);
    }

    // 释放锁，并唤醒阻塞在锁上的线程
    public final boolean release(int arg) {
        // 如果释放锁成功，返回true
        if (tryRelease(arg)) {

            Node h = head;
            // 释放成功后，唤醒head的后继节点
            // head必须不等于0，0说明已经释放过了，不能重复释放
            if (h != null && h.waitStatus != 0) unparkSuccessor(h);
            return true;
        }
        // 释放失败
        return false;
    }


    public final void acquireShared(int arg) {
        if (tryAcquireShared(arg) < 0) doAcquireShared(arg);
    }


    public final void acquireSharedInterruptibly(int arg) throws InterruptedException {
        // 线程中断
        if (Thread.interrupted()) throw new InterruptedException();
        // 如果小于0，就获取锁失败了，加入同步队列自旋等待
        if (tryAcquireShared(arg) < 0) doAcquireSharedInterruptibly(arg);

    }


    public final boolean tryAcquireSharedNanos(int arg, long nanosTimeout) throws InterruptedException {
        if (Thread.interrupted()) throw new InterruptedException();
        return tryAcquireShared(arg) >= 0 || doAcquireSharedNanos(arg, nanosTimeout);
    }

    // 让当前线程释放它所持有的共享锁
    public final boolean releaseShared(int arg) {
        // 尝试释放共享锁
        if (tryReleaseShared(arg)) {
            // 尝试成功，唤醒等待队列中的节点，从 head 开始
            doReleaseShared();
            return true;
        }
        return false;
    }

    // Queue inspection methods


    // 判断当前线程有没有前驱节点
    public final boolean hasQueuedPredecessors() {
        Node t = tail; // Read fields in reverse initialization order
        Node h = head;
        Node s;
        // 头尾节点不是一个 && 头节点的后继节点为空 || 头节点的后继节点不是当前线程
        return h != t && ((s = h.next) == null || s.thread != Thread.currentThread());
    }


    // Instrumentation and monitoring methods

    final boolean isOnSyncQueue(Node node) {
        // CONDITION || 前继节点不存在 说明不在队列中
        if (node.waitStatus == Node.CONDITION || node.prev == null) return false;
        // 如果有后继节点, 说明在队列上
        if (node.next != null) return true;
        // 从 tail 开始遍历节点，找到说明也在队列上
        return findNodeFromTail(node);
    }


    private boolean findNodeFromTail(Node node) {
        Node t = tail;
        for (; ; ) {
            if (t == node) return true;
            if (t == null) return false;
            t = t.prev;
        }
    }

    // 先CAS修改节点状态，成功就将这个节点放到队列中
    // 然后唤醒这个节点，此时节点就会在 await 方法中苏醒
    // 并在执行 checkInterruptWhileWaiting 方法后开始尝试获取锁
    final boolean transferForSignal(Node node) {

        // 尝试将Node从CONDITION置为0，失败直接返回false，说明被取消
        if (!compareAndSetWaitStatus(node, Node.CONDITION, 0)) return false;
        // 入AQS队列，返回前继节点
        Node p = enq(node);
        int ws = p.waitStatus;
        // 如果前继节点是取消状态，或者尝试前继节点通过CAS置为SIGNAL失败，就唤醒节点
        if (ws > 0 || !compareAndSetWaitStatus(p, ws, Node.SIGNAL)) LockSupport.unpark(node.thread);
        // 全部成功，返回true，唤醒等待节点成功
        return true;
    }

    // 尝试将自己放入到队列中，如果无法放入，就自旋等待 signal 方法放入
    final boolean transferAfterCancelledWait(Node node) {
        // 将节点的状态由CONDITION设成0成功后，将节点入队列
        if (compareAndSetWaitStatus(node, Node.CONDITION, 0)) {
            enq(node);
            return true;
        }
        // 如果 CAS 失败，返回 false
        // 如果节点不在AQS队列上，就自旋直到signal 中 enq让节点入队
        while (!isOnSyncQueue(node)) Thread.yield();
        return false;
    }


    final int fullyRelease(Node node) {
        boolean failed = true;
        try {
            int savedState = getState();
            // 某线程可能多次调用了lock()方法，比如10次，就将state加到10
            // 要将状态全部释放，后面的节点才能重新从state=0开始竞争锁
            // 如果释放成功，则返回之前持有锁的线程的数量
            if (release(savedState)) {
                failed = false;
                return savedState;
            } else {
                throw new IllegalMonitorStateException();
            }
        } finally {
            // 释放失败，将节点设成CANCELLED，随后将从队列中移除
            if (failed) node.waitStatus = Node.CANCELLED;
        }
    }

    // Instrumentation methods for conditions


    public class ConditionObject implements Condition {

        // 第一个等待的节点
        private transient Node firstWaiter;

        // 最后一个等待的节点
        private transient Node lastWaiter;


        public ConditionObject() {
        }

        // 添加节点到Condition等待队列 单向链表
        private Node addConditionWaiter() {
            Node t = lastWaiter;

            // 如果最后一个节点失效了就清除链表中所有失效节点
            if (t != null && t.waitStatus != Node.CONDITION) {
                unlinkCancelledWaiters();
                t = lastWaiter;
            }
            // 创建一个节点
            Node node = new Node(Thread.currentThread(), Node.CONDITION);
            // 如果最后一个节点是 null 当前节点设置成第一个节点
            if (t == null) firstWaiter = node;
                // 如果不是 null 将当前节点追加到最后一个节点
            else t.nextWaiter = node;
            // 将当前节点设置成最后一个节点
            lastWaiter = node;
            return node;
        }


        private void doSignal(Node first) {
            // first是第一个等待节点
            do {
                // 设置firstWaiter指向第一个等待节点的nextWaiter
                // 如果第一个等待节点的后继节点是 null，说明当前队列中只有一个waiter，lastWaiter置空
                if ((firstWaiter = first.nextWaiter) == null) lastWaiter = null;
                // 第一个等待节点是要被signal的，无用了，nextWaiter置空
                first.nextWaiter = null;
            } while (!transferForSignal(first) && (first = firstWaiter) != null);
            // 将节点从Condition队列转到AQS队列
            // await()的节点被唤醒之后不意味着它后面的代码会马上执行
            // 它会被加到AQS队尾 只有前面等待的节点获取锁全部完毕才轮到它

        }


        private void doSignalAll(Node first) {
            lastWaiter = firstWaiter = null;
            do {
                Node next = first.nextWaiter;
                first.nextWaiter = null;
                transferForSignal(first);
                first = next;
            } while (first != null);
        }

        // 清除链表中所有失效的节点
        private void unlinkCancelledWaiters() {
            Node t = firstWaiter;
            // 当 next 正常的时候,需要保存这个 next, 方便下次循环是链接到下一个节点上.
            Node trail = null;
            while (t != null) {
                Node next = t.nextWaiter;
                // 如果这个节点被取消了
                if (t.waitStatus != Node.CONDITION) {
                    // 先将他的 next 节点设置为 null
                    t.nextWaiter = null;
                    // 如果这是第一次判断 trail 变量 // 将 next 变量设置为 first, 也就是去除之前的 first(由于是第一次,肯定去除的是 first)
                    if (trail == null) firstWaiter = next;
                        // 如果不是 null,说明上个节点正常,将上个节点的 next 设置为无效节点的 next, 让 t 失效
                    else trail.nextWaiter = next;
                    // 如果 next 是 null, 说明没有节点了,那么就可以将 trail 设置成最后一个节点
                    if (next == null) lastWaiter = trail;
                } else
                    // 如果该节点正常,那么就保存这个节点,在下次链接下个节点时使用
                    trail = t;
                // 换下一个节点继续循环
                t = next;
            }
        }


        public final void signal() {
            // 当前线程没有独占锁
            if (!isHeldExclusively()) throw new IllegalMonitorStateException();
            Node first = firstWaiter;
            // 唤醒Condition等待队列中的第一个节点
            if (first != null) doSignal(first);
        }

        // 将Condition等待队列的所有节点已到AQS队列中
        public final void signalAll() {
            if (!isHeldExclusively()) throw new IllegalMonitorStateException();
            Node first = firstWaiter;
            if (first != null) doSignalAll(first);
        }

        public final void awaitUninterruptibly() {
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            boolean interrupted = false;
            while (!isOnSyncQueue(node)) {
                LockSupport.park(this);
                if (Thread.interrupted()) interrupted = true;
            }
            if (acquireQueued(node, savedState) || interrupted) selfInterrupt();
        }


        // 从等待退出时再次中断
        private static final int REINTERRUPT = 1;

        // 从等待退出时抛出中断异常
        private static final int THROW_IE = -1;


        private int checkInterruptWhileWaiting(Node node) {
            // 检查中断
            // 返回THROW_IE 如果在signalled之前中断
            // 返回REINTERRUPT 如果在signalled之后中断
            // 返回0 如果没中断
            return Thread.interrupted() ? (transferAfterCancelledWait(node) ? THROW_IE : REINTERRUPT) : 0;
        }


        private void reportInterruptAfterWait(int interruptMode) throws InterruptedException {
            if (interruptMode == THROW_IE) throw new InterruptedException();
            else if (interruptMode == REINTERRUPT) selfInterrupt();
        }


        public final void await() throws InterruptedException {
            // 线程被中断则抛出中断异常
            if (Thread.interrupted()) throw new InterruptedException();
            // 将节点加到 Condition 队尾
            Node node = addConditionWaiter();
            // 释放锁，并唤醒 AQS 队列中一个线程 返回之前的同步状态
            int savedState = fullyRelease(node);
            int interruptMode = 0;
            // 判断这个节点是否在 AQS 队列上 第一次判断总是返回 false 进入 while 调用 park 方法，阻塞自己
            // 这里 Condition 成功的释放了所在的 Lock 锁，并将自己阻塞
            while (!isOnSyncQueue(node)) {
                LockSupport.park(this);
                // 唤醒后检查节点在处于等待状态时是否被中断
                // 如果没有中断，则再次循环
                // 如果被中断了，就跳出循环
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0) break;
            }
            // 在跳出了循环，即被signal唤醒后重新加入了同步队列后，开始重新竞争锁

            // 拿到锁
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE) interruptMode = REINTERRUPT;
            // clean up if cancelled
            // 如果节点从等待状态转换为在同步队列中，并且也已经获得了锁，此时将断开此节点后面的等待节点
            // 如果节点的后继节点不是 null，则清理 Condition 队列上的节点
            if (node.nextWaiter != null) unlinkCancelledWaiters();
            // 如果线程被中断了，需要抛出异常，或什么都不做
            // 如果中断发生在 signal 操作之前，await 方法必须在重新获取到锁后，抛出 InterruptedException
            // 但是，如果中断发生在 signal 后，await 必须返回且不抛异常，同时设置线程的中断状态
            if (interruptMode != 0) reportInterruptAfterWait(interruptMode);
        }


        public final long awaitNanos(long nanosTimeout) throws InterruptedException {
            if (Thread.interrupted()) throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            final long deadline = System.nanoTime() + nanosTimeout;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (nanosTimeout <= 0L) {
                    transferAfterCancelledWait(node);
                    break;
                }
                if (nanosTimeout >= spinForTimeoutThreshold) LockSupport.parkNanos(this, nanosTimeout);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0) break;
                nanosTimeout = deadline - System.nanoTime();
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE) interruptMode = REINTERRUPT;
            if (node.nextWaiter != null) unlinkCancelledWaiters();
            if (interruptMode != 0) reportInterruptAfterWait(interruptMode);
            return deadline - System.nanoTime();
        }


        public final boolean awaitUntil(Date deadline) throws InterruptedException {
            long abstime = deadline.getTime();
            if (Thread.interrupted()) throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            boolean timedout = false;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (System.currentTimeMillis() > abstime) {
                    timedout = transferAfterCancelledWait(node);
                    break;
                }
                LockSupport.parkUntil(this, abstime);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0) break;
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE) interruptMode = REINTERRUPT;
            if (node.nextWaiter != null) unlinkCancelledWaiters();
            if (interruptMode != 0) reportInterruptAfterWait(interruptMode);
            return !timedout;
        }


        public final boolean await(long time, TimeUnit unit) throws InterruptedException {
            long nanosTimeout = unit.toNanos(time);
            if (Thread.interrupted()) throw new InterruptedException();
            Node node = addConditionWaiter();
            int savedState = fullyRelease(node);
            final long deadline = System.nanoTime() + nanosTimeout;
            boolean timedout = false;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                if (nanosTimeout <= 0L) {
                    timedout = transferAfterCancelledWait(node);
                    break;
                }
                if (nanosTimeout >= spinForTimeoutThreshold) LockSupport.parkNanos(this, nanosTimeout);
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0) break;
                nanosTimeout = deadline - System.nanoTime();
            }
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE) interruptMode = REINTERRUPT;
            if (node.nextWaiter != null) unlinkCancelledWaiters();
            if (interruptMode != 0) reportInterruptAfterWait(interruptMode);
            return !timedout;
        }
    }


    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static final long stateOffset;
    private static final long headOffset;
    private static final long tailOffset;
    private static final long waitStatusOffset;
    private static final long nextOffset;

    static {
        try {
            stateOffset = unsafe.objectFieldOffset(AbstractQueuedSynchronizer.class.getDeclaredField("state"));
            headOffset = unsafe.objectFieldOffset(AbstractQueuedSynchronizer.class.getDeclaredField("head"));
            tailOffset = unsafe.objectFieldOffset(AbstractQueuedSynchronizer.class.getDeclaredField("tail"));
            waitStatusOffset = unsafe.objectFieldOffset(Node.class.getDeclaredField("waitStatus"));
            nextOffset = unsafe.objectFieldOffset(Node.class.getDeclaredField("next"));
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    private final boolean compareAndSetHead(Node update) {
        return unsafe.compareAndSwapObject(this, headOffset, null, update);
    }


    private final boolean compareAndSetTail(Node expect, Node update) {
        return unsafe.compareAndSwapObject(this, tailOffset, expect, update);
    }


    private static final boolean compareAndSetWaitStatus(Node node, int expect, int update) {
        return unsafe.compareAndSwapInt(node, waitStatusOffset, expect, update);
    }


    private static final boolean compareAndSetNext(Node node, Node expect, Node update) {
        return unsafe.compareAndSwapObject(node, nextOffset, expect, update);
    }
}
