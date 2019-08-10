package com.alipay.remoting.rpc;

import com.alipay.remoting.InvokeFuture;
import com.alipay.remoting.exception.RemotingException;
import com.alipay.remoting.rpc.exception.InvokeTimeoutException;

public class RpcResponseFuture {

    private String addr;

    private InvokeFuture future;

    public RpcResponseFuture(String addr, InvokeFuture future) {
        this.addr = addr;
        this.future = future;
    }

    public boolean isDone() {
        return this.future.isDone();
    }

    public Object get(int timeoutMillis) throws InvokeTimeoutException, RemotingException, InterruptedException {
        this.future.waitResponse(timeoutMillis);
        if (!isDone()) {
            throw new InvokeTimeoutException("Future get result timeout!");
        }
        ResponseCommand responseCommand = (ResponseCommand) this.future.waitResponse();
        responseCommand.setInvokeContext(this.future.getInvokeContext());
        return RpcResponseResolver.resolveResponseObject(responseCommand, addr);
    }

    public Object get() throws RemotingException, InterruptedException {
        ResponseCommand responseCommand = (ResponseCommand) this.future.waitResponse();
        responseCommand.setInvokeContext(this.future.getInvokeContext());
        return RpcResponseResolver.resolveResponseObject(responseCommand, addr);
    }

}
