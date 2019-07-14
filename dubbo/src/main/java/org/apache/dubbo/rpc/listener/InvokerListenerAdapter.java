package org.apache.dubbo.rpc.listener;

import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.InvokerListener;
import org.apache.dubbo.rpc.RpcException;

public abstract class InvokerListenerAdapter implements InvokerListener {

    @Override
    public void referred(Invoker<?> invoker) throws RpcException {
    }

    @Override
    public void destroyed(Invoker<?> invoker) {
    }

}
