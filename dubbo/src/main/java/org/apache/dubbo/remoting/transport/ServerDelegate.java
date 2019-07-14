package org.apache.dubbo.remoting.transport;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.Server;

import java.net.InetSocketAddress;
import java.util.Collection;

public class ServerDelegate implements Server {

    private transient Server server;

    public ServerDelegate() {
    }

    public ServerDelegate(Server server) {
        setServer(server);
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
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
    public void close(int timeout) {
        server.close(timeout);
    }

    @Override
    public void startClose() {
        server.startClose();
    }

    @Override
    public boolean isClosed() {
        return server.isClosed();
    }

}
