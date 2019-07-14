package org.apache.dubbo.remoting.transport;

import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;

public class ChannelHandlerAdapter implements ChannelHandler {

    @Override
    public void connected(Channel channel) throws RemotingException {
    }

    @Override
    public void disconnected(Channel channel) throws RemotingException {
    }

    @Override
    public void sent(Channel channel, Object message) throws RemotingException {
    }

    @Override
    public void received(Channel channel, Object message) throws RemotingException {
    }

    @Override
    public void caught(Channel channel, Throwable exception) throws RemotingException {
    }

}
