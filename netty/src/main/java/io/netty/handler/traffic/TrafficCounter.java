package io.netty.handler.traffic;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class TrafficCounter {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(TrafficCounter.class);

    public static long milliSecondFromNano() {
        return System.nanoTime() / 1000000;
    }

    private final AtomicLong currentWrittenBytes = new AtomicLong();

    private final AtomicLong currentReadBytes = new AtomicLong();

    private long writingTime;

    private long readingTime;

    private final AtomicLong cumulativeWrittenBytes = new AtomicLong();

    private final AtomicLong cumulativeReadBytes = new AtomicLong();

    private long lastCumulativeTime;

    private long lastWriteThroughput;

    private long lastReadThroughput;

    final AtomicLong lastTime = new AtomicLong();

    private volatile long lastWrittenBytes;

    private volatile long lastReadBytes;

    private volatile long lastWritingTime;

    private volatile long lastReadingTime;

    private final AtomicLong realWrittenBytes = new AtomicLong();

    private long realWriteThroughput;

    final AtomicLong checkInterval = new AtomicLong(AbstractTrafficShapingHandler.DEFAULT_CHECK_INTERVAL);

    final String name;

    final AbstractTrafficShapingHandler trafficShapingHandler;

    final ScheduledExecutorService executor;

    Runnable monitor;

    volatile ScheduledFuture<?> scheduledFuture;

    volatile boolean monitorActive;

    private final class TrafficMonitoringTask implements Runnable {
        @Override
        public void run() {
            if (!monitorActive) {
                return;
            }
            resetAccounting(milliSecondFromNano());
            if (trafficShapingHandler != null) {
                trafficShapingHandler.doAccounting(TrafficCounter.this);
            }
            scheduledFuture = executor.schedule(this, checkInterval.get(), TimeUnit.MILLISECONDS);
        }
    }

    public synchronized void start() {
        if (monitorActive) {
            return;
        }
        lastTime.set(milliSecondFromNano());
        long localCheckInterval = checkInterval.get();

        if (localCheckInterval > 0 && executor != null) {
            monitorActive = true;
            monitor = new TrafficMonitoringTask();
            scheduledFuture = executor.schedule(monitor, localCheckInterval, TimeUnit.MILLISECONDS);
        }
    }

    public synchronized void stop() {
        if (!monitorActive) {
            return;
        }
        monitorActive = false;
        resetAccounting(milliSecondFromNano());
        if (trafficShapingHandler != null) {
            trafficShapingHandler.doAccounting(this);
        }
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }
    }

    synchronized void resetAccounting(long newLastTime) {
        long interval = newLastTime - lastTime.getAndSet(newLastTime);
        if (interval == 0) {

            return;
        }
        if (logger.isDebugEnabled() && interval > checkInterval() << 1) {
            logger.debug("Acct schedule not ok: " + interval + " > 2*" + checkInterval() + " from " + name);
        }
        lastReadBytes = currentReadBytes.getAndSet(0);
        lastWrittenBytes = currentWrittenBytes.getAndSet(0);
        lastReadThroughput = lastReadBytes * 1000 / interval;

        lastWriteThroughput = lastWrittenBytes * 1000 / interval;

        realWriteThroughput = realWrittenBytes.getAndSet(0) * 1000 / interval;
        lastWritingTime = Math.max(lastWritingTime, writingTime);
        lastReadingTime = Math.max(lastReadingTime, readingTime);
    }

    public TrafficCounter(ScheduledExecutorService executor, String name, long checkInterval) {
        if (name == null) {
            throw new NullPointerException("name");
        }

        trafficShapingHandler = null;
        this.executor = executor;
        this.name = name;

        init(checkInterval);
    }

    public TrafficCounter(AbstractTrafficShapingHandler trafficShapingHandler, ScheduledExecutorService executor, String name, long checkInterval) {

        if (trafficShapingHandler == null) {
            throw new IllegalArgumentException("trafficShapingHandler");
        }
        if (name == null) {
            throw new NullPointerException("name");
        }

        this.trafficShapingHandler = trafficShapingHandler;
        this.executor = executor;
        this.name = name;

        init(checkInterval);
    }

    private void init(long checkInterval) {

        lastCumulativeTime = System.currentTimeMillis();
        writingTime = milliSecondFromNano();
        readingTime = writingTime;
        lastWritingTime = writingTime;
        lastReadingTime = writingTime;
        configure(checkInterval);
    }

    public void configure(long newCheckInterval) {
        long newInterval = newCheckInterval / 10 * 10;
        if (checkInterval.getAndSet(newInterval) != newInterval) {
            if (newInterval <= 0) {
                stop();

                lastTime.set(milliSecondFromNano());
            } else {

                start();
            }
        }
    }

    void bytesRecvFlowControl(long recv) {
        currentReadBytes.addAndGet(recv);
        cumulativeReadBytes.addAndGet(recv);
    }

    void bytesWriteFlowControl(long write) {
        currentWrittenBytes.addAndGet(write);
        cumulativeWrittenBytes.addAndGet(write);
    }

    void bytesRealWriteFlowControl(long write) {
        realWrittenBytes.addAndGet(write);
    }

    public long checkInterval() {
        return checkInterval.get();
    }

    public long lastReadThroughput() {
        return lastReadThroughput;
    }

    public long lastWriteThroughput() {
        return lastWriteThroughput;
    }

    public long lastReadBytes() {
        return lastReadBytes;
    }

    public long lastWrittenBytes() {
        return lastWrittenBytes;
    }

    public long currentReadBytes() {
        return currentReadBytes.get();
    }

    public long currentWrittenBytes() {
        return currentWrittenBytes.get();
    }

    public long lastTime() {
        return lastTime.get();
    }

    public long cumulativeWrittenBytes() {
        return cumulativeWrittenBytes.get();
    }

    public long cumulativeReadBytes() {
        return cumulativeReadBytes.get();
    }

    public long lastCumulativeTime() {
        return lastCumulativeTime;
    }

    public AtomicLong getRealWrittenBytes() {
        return realWrittenBytes;
    }

    public long getRealWriteThroughput() {
        return realWriteThroughput;
    }

    public void resetCumulativeTime() {
        lastCumulativeTime = System.currentTimeMillis();
        cumulativeReadBytes.set(0);
        cumulativeWrittenBytes.set(0);
    }

    public String name() {
        return name;
    }

    @Deprecated
    public long readTimeToWait(final long size, final long limitTraffic, final long maxTime) {
        return readTimeToWait(size, limitTraffic, maxTime, milliSecondFromNano());
    }

    public long readTimeToWait(final long size, final long limitTraffic, final long maxTime, final long now) {
        bytesRecvFlowControl(size);
        if (size == 0 || limitTraffic == 0) {
            return 0;
        }
        final long lastTimeCheck = lastTime.get();
        long sum = currentReadBytes.get();
        long localReadingTime = readingTime;
        long lastRB = lastReadBytes;
        final long interval = now - lastTimeCheck;
        long pastDelay = Math.max(lastReadingTime - lastTimeCheck, 0);
        if (interval > AbstractTrafficShapingHandler.MINIMAL_WAIT) {

            long time = sum * 1000 / limitTraffic - interval + pastDelay;
            if (time > AbstractTrafficShapingHandler.MINIMAL_WAIT) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Time: " + time + ':' + sum + ':' + interval + ':' + pastDelay);
                }
                if (time > maxTime && now + time - localReadingTime > maxTime) {
                    time = maxTime;
                }
                readingTime = Math.max(localReadingTime, now + time);
                return time;
            }
            readingTime = Math.max(localReadingTime, now);
            return 0;
        }

        long lastsum = sum + lastRB;
        long lastinterval = interval + checkInterval.get();
        long time = lastsum * 1000 / limitTraffic - lastinterval + pastDelay;
        if (time > AbstractTrafficShapingHandler.MINIMAL_WAIT) {
            if (logger.isDebugEnabled()) {
                logger.debug("Time: " + time + ':' + lastsum + ':' + lastinterval + ':' + pastDelay);
            }
            if (time > maxTime && now + time - localReadingTime > maxTime) {
                time = maxTime;
            }
            readingTime = Math.max(localReadingTime, now + time);
            return time;
        }
        readingTime = Math.max(localReadingTime, now);
        return 0;
    }

    @Deprecated
    public long writeTimeToWait(final long size, final long limitTraffic, final long maxTime) {
        return writeTimeToWait(size, limitTraffic, maxTime, milliSecondFromNano());
    }

    public long writeTimeToWait(final long size, final long limitTraffic, final long maxTime, final long now) {
        bytesWriteFlowControl(size);
        if (size == 0 || limitTraffic == 0) {
            return 0;
        }
        final long lastTimeCheck = lastTime.get();
        long sum = currentWrittenBytes.get();
        long lastWB = lastWrittenBytes;
        long localWritingTime = writingTime;
        long pastDelay = Math.max(lastWritingTime - lastTimeCheck, 0);
        final long interval = now - lastTimeCheck;
        if (interval > AbstractTrafficShapingHandler.MINIMAL_WAIT) {

            long time = sum * 1000 / limitTraffic - interval + pastDelay;
            if (time > AbstractTrafficShapingHandler.MINIMAL_WAIT) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Time: " + time + ':' + sum + ':' + interval + ':' + pastDelay);
                }
                if (time > maxTime && now + time - localWritingTime > maxTime) {
                    time = maxTime;
                }
                writingTime = Math.max(localWritingTime, now + time);
                return time;
            }
            writingTime = Math.max(localWritingTime, now);
            return 0;
        }

        long lastsum = sum + lastWB;
        long lastinterval = interval + checkInterval.get();
        long time = lastsum * 1000 / limitTraffic - lastinterval + pastDelay;
        if (time > AbstractTrafficShapingHandler.MINIMAL_WAIT) {
            if (logger.isDebugEnabled()) {
                logger.debug("Time: " + time + ':' + lastsum + ':' + lastinterval + ':' + pastDelay);
            }
            if (time > maxTime && now + time - localWritingTime > maxTime) {
                time = maxTime;
            }
            writingTime = Math.max(localWritingTime, now + time);
            return time;
        }
        writingTime = Math.max(localWritingTime, now);
        return 0;
    }

    @Override
    public String toString() {
        return new StringBuilder(165).append("Monitor ").append(name).append(" Current Speed Read: ").append(lastReadThroughput >> 10).append(" KB/s, ").append("Asked Write: ").append(lastWriteThroughput >> 10).append(" KB/s, ").append("Real Write: ").append(realWriteThroughput >> 10).append(" KB/s, ").append("Current Read: ").append(currentReadBytes.get() >> 10).append(" KB, ").append("Current asked Write: ").append(currentWrittenBytes.get() >> 10).append(" KB, ").append("Current real Write: ").append(realWrittenBytes.get() >> 10).append(" KB").toString();
    }
}
