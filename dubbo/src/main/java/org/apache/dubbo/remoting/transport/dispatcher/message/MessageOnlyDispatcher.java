package org.apache.dubbo.remoting.transport.dispatcher.message;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.Dispatcher;

public class MessageOnlyDispatcher implements Dispatcher {

    public static final String NAME = "message";

    @Override
    public ChannelHandler dispatch(ChannelHandler handler, URL url) {
        return new MessageOnlyChannelHandler(handler, url);
    }

}
