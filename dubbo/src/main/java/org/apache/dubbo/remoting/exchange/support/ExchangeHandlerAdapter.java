package org.apache.dubbo.remoting.exchange.support;

import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.ExchangeChannel;
import org.apache.dubbo.remoting.exchange.ExchangeHandler;
import org.apache.dubbo.remoting.telnet.support.TelnetHandlerAdapter;

import java.util.concurrent.CompletableFuture;

public abstract class ExchangeHandlerAdapter extends TelnetHandlerAdapter implements ExchangeHandler {

    @Override
    public CompletableFuture<Object> reply(ExchangeChannel channel, Object msg) throws RemotingException {
        return null;
    }

}