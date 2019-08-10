package com.alipay.remoting;

public abstract class AbstractLifeCycle implements LifeCycle {

    private volatile boolean isStarted = false;

    @Override
    public void startup() throws LifeCycleException {
        if (!isStarted) {
            isStarted = true;
            return;
        }
        throw new LifeCycleException("this component has started");
    }

    @Override
    public void shutdown() throws LifeCycleException {
        if (isStarted) {
            isStarted = false;
            return;
        }
        throw new LifeCycleException("this component has closed");
    }

    @Override
    public boolean isStarted() {
        return isStarted;
    }

}
