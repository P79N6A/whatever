package org.apache.dubbo.rpc;

public interface AsyncContext {

    void write(Object value);

    boolean isAsyncStarted();

    boolean stop();

    void start();

    void signalContextSwitch();

}
