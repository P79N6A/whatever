package org.apache.dubbo.remoting.transport;

import org.apache.dubbo.remoting.ChannelHandler;

public interface ChannelHandlerDelegate extends ChannelHandler {
    ChannelHandler getHandler();

}
