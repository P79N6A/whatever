package org.apache.dubbo.remoting.transport.dispatcher.execution;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.Dispatcher;

public class ExecutionDispatcher implements Dispatcher {

    public static final String NAME = "execution";

    @Override
    public ChannelHandler dispatch(ChannelHandler handler, URL url) {
        return new ExecutionChannelHandler(handler, url);
    }

}
