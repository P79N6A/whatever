package com.alipay.remoting.config;

import com.alipay.remoting.ConnectionSelectStrategy;

public class BoltGenericOption<T> extends BoltOption<T> {

    public static final BoltOption<Boolean> TCP_NODELAY = valueOf("bolt.tcp.nodelay", true);

    public static final BoltOption<Boolean> TCP_SO_REUSEADDR = valueOf("bolt.tcp.so.reuseaddr", true);

    public static final BoltOption<Boolean> TCP_SO_KEEPALIVE = valueOf("bolt.tcp.so.keepalive", true);

    public static final BoltOption<Integer> NETTY_IO_RATIO = valueOf("bolt.netty.io.ratio", 70);

    public static final BoltOption<Boolean> NETTY_BUFFER_POOLED = valueOf("bolt.netty.buffer.pooled", true);

    public static final BoltOption<Integer> NETTY_BUFFER_HIGH_WATER_MARK = valueOf("bolt.netty.buffer.high.watermark", 64 * 1024);

    public static final BoltOption<Integer> NETTY_BUFFER_LOW_WATER_MARK = valueOf("bolt.netty.buffer.low.watermark", 32 * 1024);

    public static final BoltOption<Boolean> NETTY_EPOLL_SWITCH = valueOf("bolt.netty.epoll.switch", true);

    public static final BoltOption<Boolean> TCP_IDLE_SWITCH = valueOf("bolt.tcp.heartbeat.switch", true);

    public static final BoltOption<Integer> TP_MIN_SIZE = valueOf("bolt.tp.min", 20);

    public static final BoltOption<Integer> TP_MAX_SIZE = valueOf("bolt.tp.max", 400);

    public static final BoltOption<Integer> TP_QUEUE_SIZE = valueOf("bolt.tp.queue", 600);

    public static final BoltOption<Integer> TP_KEEPALIVE_TIME = valueOf("bolt.tp.keepalive", 60);

    public static final BoltOption<ConnectionSelectStrategy> CONNECTION_SELECT_STRATEGY = valueOf("CONNECTION_SELECT_STRATEGY");

    protected BoltGenericOption(String name, T defaultValue) {
        super(name, defaultValue);
    }

}
