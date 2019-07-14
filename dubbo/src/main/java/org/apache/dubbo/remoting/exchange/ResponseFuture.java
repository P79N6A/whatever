package org.apache.dubbo.remoting.exchange;

import org.apache.dubbo.remoting.RemotingException;

public interface ResponseFuture {

    Object get() throws RemotingException;

    Object get(int timeoutInMillis) throws RemotingException;

    void setCallback(ResponseCallback callback);

    boolean isDone();

}