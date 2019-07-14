package org.apache.dubbo.remoting;

import org.apache.dubbo.common.Resetable;

public interface Client extends Endpoint, Channel, Resetable, IdleSensible {

    void reconnect() throws RemotingException;

    @Deprecated
    void reset(org.apache.dubbo.common.Parameters parameters);

}
