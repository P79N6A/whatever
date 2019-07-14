package org.apache.dubbo.rpc;

import org.apache.dubbo.common.extension.SPI;

@SPI
public interface InvokerListener {

    void referred(Invoker<?> invoker) throws RpcException;

    void destroyed(Invoker<?> invoker);

}