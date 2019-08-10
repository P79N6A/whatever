package com.alipay.remoting;

import com.alipay.remoting.config.switches.GlobalSwitch;
import com.alipay.remoting.connection.ConnectionFactory;

public class DefaultClientConnectionManager extends DefaultConnectionManager implements ClientConnectionManager {

    public DefaultClientConnectionManager(ConnectionSelectStrategy connectionSelectStrategy, ConnectionFactory connectionFactory, ConnectionEventHandler connectionEventHandler, ConnectionEventListener connectionEventListener) {
        super(connectionSelectStrategy, connectionFactory, connectionEventHandler, connectionEventListener);
    }

    public DefaultClientConnectionManager(ConnectionSelectStrategy connectionSelectStrategy, ConnectionFactory connectionFactory, ConnectionEventHandler connectionEventHandler, ConnectionEventListener connectionEventListener, GlobalSwitch globalSwitch) {
        super(connectionSelectStrategy, connectionFactory, connectionEventHandler, connectionEventListener, globalSwitch);
    }

    @Override
    public void startup() throws LifeCycleException {
        super.startup();
        this.connectionEventHandler.setConnectionManager(this);
        this.connectionEventHandler.setConnectionEventListener(connectionEventListener);
        this.connectionFactory.init(connectionEventHandler);
    }

}
