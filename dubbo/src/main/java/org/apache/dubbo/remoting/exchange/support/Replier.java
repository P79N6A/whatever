package org.apache.dubbo.remoting.exchange.support;

import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.ExchangeChannel;

public interface Replier<T> {

    Object reply(ExchangeChannel channel, T request) throws RemotingException;

}