package org.apache.dubbo.common.threadlocal;

public class InternalThread extends Thread {

    private InternalThreadLocalMap threadLocalMap;

    public InternalThread() {
    }

    public InternalThread(Runnable target) {
        super(target);
    }

    public InternalThread(ThreadGroup group, Runnable target) {
        super(group, target);
    }

    public InternalThread(String name) {
        super(name);
    }

    public InternalThread(ThreadGroup group, String name) {
        super(group, name);
    }

    public InternalThread(Runnable target, String name) {
        super(target, name);
    }

    public InternalThread(ThreadGroup group, Runnable target, String name) {
        super(group, target, name);
    }

    public InternalThread(ThreadGroup group, Runnable target, String name, long stackSize) {
        super(group, target, name, stackSize);
    }

    public final InternalThreadLocalMap threadLocalMap() {
        return threadLocalMap;
    }

    public final void setThreadLocalMap(InternalThreadLocalMap threadLocalMap) {
        this.threadLocalMap = threadLocalMap;
    }

}
