package org.apache.dubbo.rpc.protocol.dubbo.status;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.status.Status;
import org.apache.dubbo.common.status.StatusChecker;
import org.apache.dubbo.remoting.exchange.ExchangeServer;
import org.apache.dubbo.rpc.protocol.dubbo.DubboProtocol;

import java.util.Collection;

@Activate
public class ServerStatusChecker implements StatusChecker {

    @Override
    public Status check() {
        Collection<ExchangeServer> servers = DubboProtocol.getDubboProtocol().getServers();
        if (servers == null || servers.isEmpty()) {
            return new Status(Status.Level.UNKNOWN);
        }
        Status.Level level = Status.Level.OK;
        StringBuilder buf = new StringBuilder();
        for (ExchangeServer server : servers) {
            if (!server.isBound()) {
                level = Status.Level.ERROR;
                buf.setLength(0);
                buf.append(server.getLocalAddress());
                break;
            }
            if (buf.length() > 0) {
                buf.append(",");
            }
            buf.append(server.getLocalAddress());
            buf.append("(clients:");
            buf.append(server.getChannels().size());
            buf.append(")");
        }
        return new Status(level, buf.toString());
    }

}
