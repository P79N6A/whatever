package org.apache.dubbo.remoting.exchange.support.header;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.Channel;

public class CloseTimerTask extends AbstractTimerTask {

    private static final Logger logger = LoggerFactory.getLogger(CloseTimerTask.class);

    private final int idleTimeout;

    public CloseTimerTask(ChannelProvider channelProvider, Long heartbeatTimeoutTick, int idleTimeout) {
        super(channelProvider, heartbeatTimeoutTick);
        this.idleTimeout = idleTimeout;
    }

    @Override
    protected void doTask(Channel channel) {
        try {
            Long lastRead = lastRead(channel);
            Long lastWrite = lastWrite(channel);
            Long now = now();
            if ((lastRead != null && now - lastRead > idleTimeout) || (lastWrite != null && now - lastWrite > idleTimeout)) {
                logger.warn("Close channel " + channel + ", because idleCheck timeout: " + idleTimeout + "ms");
                channel.close();
            }
        } catch (Throwable t) {
            logger.warn("Exception when close remote channel " + channel.getRemoteAddress(), t);
        }
    }

}