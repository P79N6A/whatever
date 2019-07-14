package org.apache.dubbo.rpc;

/**
 * 维护Invoker的生命周期，内部包含Invoker或者ExporterMap
 */
public interface Exporter<T> {

    Invoker<T> getInvoker();

    void unexport();

}