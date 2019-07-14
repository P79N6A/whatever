package org.apache.dubbo.remoting.exchange.support.header;

import org.apache.dubbo.common.timer.Timeout;
import org.apache.dubbo.common.timer.Timer;
import org.apache.dubbo.common.timer.TimerTask;
import org.apache.dubbo.remoting.Channel;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

public abstract class AbstractTimerTask implements TimerTask {

    private final ChannelProvider channelProvider;

    private final Long tick;

    protected volatile boolean cancel = false;

    AbstractTimerTask(ChannelProvider channelProvider, Long tick) {
        if (channelProvider == null || tick == null) {
            throw new IllegalArgumentException();
        }
        this.tick = tick;
        this.channelProvider = channelProvider;
    }

    static Long lastRead(Channel channel) {
        return (Long) channel.getAttribute(HeaderExchangeHandler.KEY_READ_TIMESTAMP);
    }

    static Long lastWrite(Channel channel) {
        return (Long) channel.getAttribute(HeaderExchangeHandler.KEY_WRITE_TIMESTAMP);
    }

    static Long now() {
        return System.currentTimeMillis();
    }

    public void cancel() {
        this.cancel = true;
    }

    private void reput(Timeout timeout, Long tick) {
        if (timeout == null || tick == null) {
            throw new IllegalArgumentException();
        }
        if (cancel) {
            return;
        }
        Timer timer = timeout.timer();
        if (timer.isStop() || timeout.isCancelled()) {
            return;
        }
        timer.newTimeout(timeout.task(), tick, TimeUnit.MILLISECONDS);
    }

    @Override
    public void run(Timeout timeout) throws Exception {
        Collection<Channel> c = channelProvider.getChannels();
        for (Channel channel : c) {
            if (channel.isClosed()) {
                continue;
            }
            doTask(channel);
        }
        reput(timeout, tick);
    }

    protected abstract void doTask(Channel channel);

    interface ChannelProvider {
        Collection<Channel> getChannels();

    }

}
