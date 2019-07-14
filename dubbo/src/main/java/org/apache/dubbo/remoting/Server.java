package org.apache.dubbo.remoting;

import org.apache.dubbo.common.Resetable;

import java.net.InetSocketAddress;
import java.util.Collection;

public interface Server extends Endpoint, Resetable, IdleSensible {

    boolean isBound();

    Collection<Channel> getChannels();

    Channel getChannel(InetSocketAddress remoteAddress);

    @Deprecated
    void reset(org.apache.dubbo.common.Parameters parameters);

}
