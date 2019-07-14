package org.apache.dubbo.rpc;

import java.util.concurrent.CompletableFuture;

public class SimpleAsyncRpcResult extends AsyncRpcResult {
    public SimpleAsyncRpcResult(CompletableFuture<Object> future, boolean registerCallback) {
        super(future, registerCallback);
    }

    public SimpleAsyncRpcResult(CompletableFuture<Object> future, CompletableFuture<Result> rFuture, boolean registerCallback) {
        super(future, rFuture, registerCallback);
    }

    @Override
    public Object recreate() throws Throwable {
        return null;
    }

}
