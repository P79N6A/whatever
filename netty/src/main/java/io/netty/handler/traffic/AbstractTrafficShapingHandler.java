package io.netty.handler.traffic;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.channel.*;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.TimeUnit;

public abstract class AbstractTrafficShapingHandler extends ChannelDuplexHandler {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractTrafficShapingHandler.class);

    public static final long DEFAULT_CHECK_INTERVAL = 1000;

    public static final long DEFAULT_MAX_TIME = 15000;

    static final long DEFAULT_MAX_SIZE = 4 * 1024 * 1024L;

    static final long MINIMAL_WAIT = 10;

    protected TrafficCounter trafficCounter;

    private volatile long writeLimit;

    private volatile long readLimit;

    protected volatile long maxTime = DEFAULT_MAX_TIME;

    protected volatile long checkInterval = DEFAULT_CHECK_INTERVAL;

    static final AttributeKey<Boolean> READ_SUSPENDED = AttributeKey.valueOf(AbstractTrafficShapingHandler.class.getName() + ".READ_SUSPENDED");
    static final AttributeKey<Runnable> REOPEN_TASK = AttributeKey.valueOf(AbstractTrafficShapingHandler.class.getName() + ".REOPEN_TASK");

    volatile long maxWriteDelay = 4 * DEFAULT_CHECK_INTERVAL;

    volatile long maxWriteSize = DEFAULT_MAX_SIZE;

    final int userDefinedWritabilityIndex;

    static final int CHANNEL_DEFAULT_USER_DEFINED_WRITABILITY_INDEX = 1;

    static final int GLOBAL_DEFAULT_USER_DEFINED_WRITABILITY_INDEX = 2;

    static final int GLOBALCHANNEL_DEFAULT_USER_DEFINED_WRITABILITY_INDEX = 3;

    void setTrafficCounter(TrafficCounter newTrafficCounter) {
        trafficCounter = newTrafficCounter;
    }

    protected int userDefinedWritabilityIndex() {
        return CHANNEL_DEFAULT_USER_DEFINED_WRITABILITY_INDEX;
    }

    protected AbstractTrafficShapingHandler(long writeLimit, long readLimit, long checkInterval, long maxTime) {
        if (maxTime <= 0) {
            throw new IllegalArgumentException("maxTime must be positive");
        }

        userDefinedWritabilityIndex = userDefinedWritabilityIndex();
        this.writeLimit = writeLimit;
        this.readLimit = readLimit;
        this.checkInterval = checkInterval;
        this.maxTime = maxTime;
    }

    protected AbstractTrafficShapingHandler(long writeLimit, long readLimit, long checkInterval) {
        this(writeLimit, readLimit, checkInterval, DEFAULT_MAX_TIME);
    }

    protected AbstractTrafficShapingHandler(long writeLimit, long readLimit) {
        this(writeLimit, readLimit, DEFAULT_CHECK_INTERVAL, DEFAULT_MAX_TIME);
    }

    protected AbstractTrafficShapingHandler() {
        this(0, 0, DEFAULT_CHECK_INTERVAL, DEFAULT_MAX_TIME);
    }

    protected AbstractTrafficShapingHandler(long checkInterval) {
        this(0, 0, checkInterval, DEFAULT_MAX_TIME);
    }

    public void configure(long newWriteLimit, long newReadLimit, long newCheckInterval) {
        configure(newWriteLimit, newReadLimit);
        configure(newCheckInterval);
    }

    public void configure(long newWriteLimit, long newReadLimit) {
        writeLimit = newWriteLimit;
        readLimit = newReadLimit;
        if (trafficCounter != null) {
            trafficCounter.resetAccounting(TrafficCounter.milliSecondFromNano());
        }
    }

    public void configure(long newCheckInterval) {
        checkInterval = newCheckInterval;
        if (trafficCounter != null) {
            trafficCounter.configure(checkInterval);
        }
    }

    public long getWriteLimit() {
        return writeLimit;
    }

    public void setWriteLimit(long writeLimit) {
        this.writeLimit = writeLimit;
        if (trafficCounter != null) {
            trafficCounter.resetAccounting(TrafficCounter.milliSecondFromNano());
        }
    }

    public long getReadLimit() {
        return readLimit;
    }

    public void setReadLimit(long readLimit) {
        this.readLimit = readLimit;
        if (trafficCounter != null) {
            trafficCounter.resetAccounting(TrafficCounter.milliSecondFromNano());
        }
    }

    public long getCheckInterval() {
        return checkInterval;
    }

    public void setCheckInterval(long checkInterval) {
        this.checkInterval = checkInterval;
        if (trafficCounter != null) {
            trafficCounter.configure(checkInterval);
        }
    }

    public void setMaxTimeWait(long maxTime) {
        if (maxTime <= 0) {
            throw new IllegalArgumentException("maxTime must be positive");
        }
        this.maxTime = maxTime;
    }

    public long getMaxTimeWait() {
        return maxTime;
    }

    public long getMaxWriteDelay() {
        return maxWriteDelay;
    }

    public void setMaxWriteDelay(long maxWriteDelay) {
        if (maxWriteDelay <= 0) {
            throw new IllegalArgumentException("maxWriteDelay must be positive");
        }
        this.maxWriteDelay = maxWriteDelay;
    }

    public long getMaxWriteSize() {
        return maxWriteSize;
    }

    public void setMaxWriteSize(long maxWriteSize) {
        this.maxWriteSize = maxWriteSize;
    }

    protected void doAccounting(TrafficCounter counter) {

    }

    static final class ReopenReadTimerTask implements Runnable {
        final ChannelHandlerContext ctx;

        ReopenReadTimerTask(ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public void run() {
            Channel channel = ctx.channel();
            ChannelConfig config = channel.config();
            if (!config.isAutoRead() && isHandlerActive(ctx)) {

                if (logger.isDebugEnabled()) {
                    logger.debug("Not unsuspend: " + config.isAutoRead() + ':' + isHandlerActive(ctx));
                }
                channel.attr(READ_SUSPENDED).set(false);
            } else {

                if (logger.isDebugEnabled()) {
                    if (config.isAutoRead() && !isHandlerActive(ctx)) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Unsuspend: " + config.isAutoRead() + ':' + isHandlerActive(ctx));
                        }
                    } else {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Normal unsuspend: " + config.isAutoRead() + ':' + isHandlerActive(ctx));
                        }
                    }
                }
                channel.attr(READ_SUSPENDED).set(false);
                config.setAutoRead(true);
                channel.read();
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Unsuspend final status => " + config.isAutoRead() + ':' + isHandlerActive(ctx));
            }
        }
    }

    void releaseReadSuspended(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        channel.attr(READ_SUSPENDED).set(false);
        channel.config().setAutoRead(true);
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        long size = calculateSize(msg);
        long now = TrafficCounter.milliSecondFromNano();
        if (size > 0) {

            long wait = trafficCounter.readTimeToWait(size, readLimit, maxTime, now);
            wait = checkWaitReadTime(ctx, wait, now);
            if (wait >= MINIMAL_WAIT) {

                Channel channel = ctx.channel();
                ChannelConfig config = channel.config();
                if (logger.isDebugEnabled()) {
                    logger.debug("Read suspend: " + wait + ':' + config.isAutoRead() + ':' + isHandlerActive(ctx));
                }
                if (config.isAutoRead() && isHandlerActive(ctx)) {
                    config.setAutoRead(false);
                    channel.attr(READ_SUSPENDED).set(true);

                    Attribute<Runnable> attr = channel.attr(REOPEN_TASK);
                    Runnable reopenTask = attr.get();
                    if (reopenTask == null) {
                        reopenTask = new ReopenReadTimerTask(ctx);
                        attr.set(reopenTask);
                    }
                    ctx.executor().schedule(reopenTask, wait, TimeUnit.MILLISECONDS);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Suspend final status => " + config.isAutoRead() + ':' + isHandlerActive(ctx) + " will reopened at: " + wait);
                    }
                }
            }
        }
        informReadOperation(ctx, now);
        ctx.fireChannelRead(msg);
    }

    long checkWaitReadTime(final ChannelHandlerContext ctx, long wait, final long now) {

        return wait;
    }

    void informReadOperation(final ChannelHandlerContext ctx, final long now) {

    }

    protected static boolean isHandlerActive(ChannelHandlerContext ctx) {
        Boolean suspended = ctx.channel().attr(READ_SUSPENDED).get();
        return suspended == null || Boolean.FALSE.equals(suspended);
    }

    @Override
    public void read(ChannelHandlerContext ctx) {
        if (isHandlerActive(ctx)) {

            ctx.read();
        }
    }

    @Override
    public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {
        long size = calculateSize(msg);
        long now = TrafficCounter.milliSecondFromNano();
        if (size > 0) {

            long wait = trafficCounter.writeTimeToWait(size, writeLimit, maxTime, now);
            if (wait >= MINIMAL_WAIT) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Write suspend: " + wait + ':' + ctx.channel().config().isAutoRead() + ':' + isHandlerActive(ctx));
                }
                submitWrite(ctx, msg, size, wait, now, promise);
                return;
            }
        }

        submitWrite(ctx, msg, size, 0, now, promise);
    }

    @Deprecated
    protected void submitWrite(final ChannelHandlerContext ctx, final Object msg, final long delay, final ChannelPromise promise) {
        submitWrite(ctx, msg, calculateSize(msg), delay, TrafficCounter.milliSecondFromNano(), promise);
    }

    abstract void submitWrite(ChannelHandlerContext ctx, Object msg, long size, long delay, long now, ChannelPromise promise);

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        setUserDefinedWritability(ctx, true);
        super.channelRegistered(ctx);
    }

    void setUserDefinedWritability(ChannelHandlerContext ctx, boolean writable) {
        ChannelOutboundBuffer cob = ctx.channel().unsafe().outboundBuffer();
        if (cob != null) {
            cob.setUserDefinedWritability(userDefinedWritabilityIndex, writable);
        }
    }

    void checkWriteSuspend(ChannelHandlerContext ctx, long delay, long queueSize) {
        if (queueSize > maxWriteSize || delay > maxWriteDelay) {
            setUserDefinedWritability(ctx, false);
        }
    }

    void releaseWriteSuspended(ChannelHandlerContext ctx) {
        setUserDefinedWritability(ctx, true);
    }

    public TrafficCounter trafficCounter() {
        return trafficCounter;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(290).append("TrafficShaping with Write Limit: ").append(writeLimit).append(" Read Limit: ").append(readLimit).append(" CheckInterval: ").append(checkInterval).append(" maxDelay: ").append(maxWriteDelay).append(" maxSize: ").append(maxWriteSize).append(" and Counter: ");
        if (trafficCounter != null) {
            builder.append(trafficCounter);
        } else {
            builder.append("none");
        }
        return builder.toString();
    }

    protected long calculateSize(Object msg) {
        if (msg instanceof ByteBuf) {
            return ((ByteBuf) msg).readableBytes();
        }
        if (msg instanceof ByteBufHolder) {
            return ((ByteBufHolder) msg).content().readableBytes();
        }
        return -1;
    }
}
