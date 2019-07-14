package org.apache.dubbo.remoting.exchange.support.header;

import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.exchange.Request;

public class HeartbeatTimerTask extends AbstractTimerTask {

    private static final Logger logger = LoggerFactory.getLogger(HeartbeatTimerTask.class);

    private final int heartbeat;

    HeartbeatTimerTask(ChannelProvider channelProvider, Long heartbeatTick, int heartbeat) {
        super(channelProvider, heartbeatTick);
        this.heartbeat = heartbeat;
    }

    @Override
    protected void doTask(Channel channel) {
        try {
            Long lastRead = lastRead(channel);
            Long lastWrite = lastWrite(channel);
            if ((lastRead != null && now() - lastRead > heartbeat) || (lastWrite != null && now() - lastWrite > heartbeat)) {
                Request req = new Request();
                req.setVersion(Version.getProtocolVersion());
                req.setTwoWay(true);
                req.setEvent(Request.HEARTBEAT_EVENT);
                channel.send(req);
                if (logger.isDebugEnabled()) {
                    logger.debug("Send heartbeat to remote channel " + channel.getRemoteAddress() + ", cause: The channel has no data-transmission exceeds a heartbeat period: " + heartbeat + "ms");
                }
            }
        } catch (Throwable t) {
            logger.warn("Exception when heartbeat to remote channel " + channel.getRemoteAddress(), t);
        }
    }

}
