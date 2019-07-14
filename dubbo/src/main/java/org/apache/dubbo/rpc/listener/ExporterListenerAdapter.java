package org.apache.dubbo.rpc.listener;

import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.ExporterListener;
import org.apache.dubbo.rpc.RpcException;

public abstract class ExporterListenerAdapter implements ExporterListener {

    @Override
    public void exported(Exporter<?> exporter) throws RpcException {
    }

    @Override
    public void unexported(Exporter<?> exporter) throws RpcException {
    }

}
