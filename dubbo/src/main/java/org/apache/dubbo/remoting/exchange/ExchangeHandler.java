package org.apache.dubbo.remoting.exchange;

import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.telnet.TelnetHandler;

import java.util.concurrent.CompletableFuture;

public interface ExchangeHandler extends ChannelHandler, TelnetHandler {

    CompletableFuture<Object> reply(ExchangeChannel channel, Object request) throws RemotingException;

}