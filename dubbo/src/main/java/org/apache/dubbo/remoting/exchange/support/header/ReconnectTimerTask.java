package org.apache.dubbo.remoting.exchange.support.header;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.Client;

public class ReconnectTimerTask extends AbstractTimerTask {

    private static final Logger logger = LoggerFactory.getLogger(ReconnectTimerTask.class);

    private final int idleTimeout;

    public ReconnectTimerTask(ChannelProvider channelProvider, Long heartbeatTimeoutTick, int idleTimeout) {
        super(channelProvider, heartbeatTimeoutTick);
        this.idleTimeout = idleTimeout;
    }

    @Override
    protected void doTask(Channel channel) {
        try {
            Long lastRead = lastRead(channel);
            Long now = now();
            if (!channel.isConnected()) {
                try {
                    logger.info("Initial connection to " + channel);
                    ((Client) channel).reconnect();
                } catch (Exception e) {
                    logger.error("Fail to connect to " + channel, e);
                }

            } else if (lastRead != null && now - lastRead > idleTimeout) {
                logger.warn("Reconnect to channel " + channel + ", because heartbeat read idle time out: " + idleTimeout + "ms");
                try {
                    ((Client) channel).reconnect();
                } catch (Exception e) {
                    logger.error(channel + "reconnect failed during idle time.", e);
                }
            }
        } catch (Throwable t) {
            logger.warn("Exception when reconnect to remote channel " + channel.getRemoteAddress(), t);
        }
    }

}
