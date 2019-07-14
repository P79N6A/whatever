package org.apache.dubbo.remoting.exchange;

import org.apache.dubbo.remoting.Server;

import java.net.InetSocketAddress;
import java.util.Collection;

public interface ExchangeServer extends Server {

    Collection<ExchangeChannel> getExchangeChannels();

    ExchangeChannel getExchangeChannel(InetSocketAddress remoteAddress);

}