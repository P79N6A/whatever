package io.netty.handler.traffic;

import io.netty.handler.traffic.GlobalChannelTrafficShapingHandler.PerChannel;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GlobalChannelTrafficCounter extends TrafficCounter {

    public GlobalChannelTrafficCounter(GlobalChannelTrafficShapingHandler trafficShapingHandler, ScheduledExecutorService executor, String name, long checkInterval) {
        super(trafficShapingHandler, executor, name, checkInterval);
        if (executor == null) {
            throw new IllegalArgumentException("Executor must not be null");
        }
    }

    private static class MixedTrafficMonitoringTask implements Runnable {

        private final GlobalChannelTrafficShapingHandler trafficShapingHandler1;

        private final TrafficCounter counter;

        MixedTrafficMonitoringTask(GlobalChannelTrafficShapingHandler trafficShapingHandler, TrafficCounter counter) {
            trafficShapingHandler1 = trafficShapingHandler;
            this.counter = counter;
        }

        @Override
        public void run() {
            if (!counter.monitorActive) {
                return;
            }
            long newLastTime = milliSecondFromNano();
            counter.resetAccounting(newLastTime);
            for (PerChannel perChannel : trafficShapingHandler1.channelQueues.values()) {
                perChannel.channelTrafficCounter.resetAccounting(newLastTime);
            }
            trafficShapingHandler1.doAccounting(counter);
            counter.scheduledFuture = counter.executor.schedule(this, counter.checkInterval.get(), TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public synchronized void start() {
        if (monitorActive) {
            return;
        }
        lastTime.set(milliSecondFromNano());
        long localCheckInterval = checkInterval.get();
        if (localCheckInterval > 0) {
            monitorActive = true;
            monitor = new MixedTrafficMonitoringTask((GlobalChannelTrafficShapingHandler) trafficShapingHandler, this);
            scheduledFuture = executor.schedule(monitor, localCheckInterval, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public synchronized void stop() {
        if (!monitorActive) {
            return;
        }
        monitorActive = false;
        resetAccounting(milliSecondFromNano());
        trafficShapingHandler.doAccounting(this);
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }
    }

    @Override
    public void resetCumulativeTime() {
        for (PerChannel perChannel : ((GlobalChannelTrafficShapingHandler) trafficShapingHandler).channelQueues.values()) {
            perChannel.channelTrafficCounter.resetCumulativeTime();
        }
        super.resetCumulativeTime();
    }

}
