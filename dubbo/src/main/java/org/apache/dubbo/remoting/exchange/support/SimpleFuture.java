package org.apache.dubbo.remoting.exchange.support;

import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.ResponseCallback;
import org.apache.dubbo.remoting.exchange.ResponseFuture;

public class SimpleFuture implements ResponseFuture {

    private final Object value;

    public SimpleFuture(Object value) {
        this.value = value;
    }

    @Override
    public Object get() throws RemotingException {
        return value;
    }

    @Override
    public Object get(int timeoutInMillis) throws RemotingException {
        return value;
    }

    @Override
    public void setCallback(ResponseCallback callback) {
        callback.done(value);
    }

    @Override
    public boolean isDone() {
        return true;
    }

}
