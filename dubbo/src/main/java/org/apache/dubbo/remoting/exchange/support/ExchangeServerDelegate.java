package org.apache.dubbo.remoting.exchange.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.ExchangeChannel;
import org.apache.dubbo.remoting.exchange.ExchangeServer;

import java.net.InetSocketAddress;
import java.util.Collection;

public class ExchangeServerDelegate implements ExchangeServer {

    private transient ExchangeServer server;

    public ExchangeServerDelegate() {
    }

    public ExchangeServerDelegate(ExchangeServer server) {
        setServer(server);
    }

    public ExchangeServer getServer() {
        return server;
    }

    public void setServer(ExchangeServer server) {
        this.server = server;
    }

    @Override
    public boolean isBound() {
        return server.isBound();
    }

    @Override
    public void reset(URL url) {
        server.reset(url);
    }

    @Override
    @Deprecated
    public void reset(org.apache.dubbo.common.Parameters parameters) {
        reset(getUrl().addParameters(parameters.getParameters()));
    }

    @Override
    public Collection<Channel> getChannels() {
        return server.getChannels();
    }

    @Override
    public Channel getChannel(InetSocketAddress remoteAddress) {
        return server.getChannel(remoteAddress);
    }

    @Override
    public URL getUrl() {
        return server.getUrl();
    }

    @Override
    public ChannelHandler getChannelHandler() {
        return server.getChannelHandler();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return server.getLocalAddress();
    }

    @Override
    public void send(Object message) throws RemotingException {
        server.send(message);
    }

    @Override
    public void send(Object message, boolean sent) throws RemotingException {
        server.send(message, sent);
    }

    @Override
    public void close() {
        server.close();
    }

    @Override
    public boolean isClosed() {
        return server.isClosed();
    }

    @Override
    public Collection<ExchangeChannel> getExchangeChannels() {
        return server.getExchangeChannels();
    }

    @Override
    public ExchangeChannel getExchangeChannel(InetSocketAddress remoteAddress) {
        return server.getExchangeChannel(remoteAddress);
    }

    @Override
    public void close(int timeout) {
        server.close(timeout);
    }

    @Override
    public void startClose() {
        server.startClose();
    }

}
