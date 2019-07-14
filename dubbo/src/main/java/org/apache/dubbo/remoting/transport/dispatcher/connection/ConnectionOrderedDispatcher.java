package org.apache.dubbo.remoting.transport.dispatcher.connection;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.Dispatcher;

public class ConnectionOrderedDispatcher implements Dispatcher {

    public static final String NAME = "connection";

    @Override
    public ChannelHandler dispatch(ChannelHandler handler, URL url) {
        return new ConnectionOrderedChannelHandler(handler, url);
    }

}
