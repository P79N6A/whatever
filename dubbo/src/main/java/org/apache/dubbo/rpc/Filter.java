package org.apache.dubbo.rpc;

import org.apache.dubbo.common.extension.SPI;

@SPI
public interface Filter {

    Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException;

    default Result onResponse(Result result, Invoker<?> invoker, Invocation invocation) {
        return result;
    }

}