package com.alipay.remoting.config;

public class BoltClientOption<T> extends BoltGenericOption<T> {

    public static final BoltOption<Integer> NETTY_IO_RATIO = valueOf("bolt.tcp.heartbeat.interval", 15 * 1000);

    public static final BoltOption<Integer> TCP_IDLE_MAXTIMES = valueOf("bolt.tcp.heartbeat.maxtimes", 3);

    public static final BoltOption<Integer> CONN_CREATE_TP_MIN_SIZE = valueOf("bolt.conn.create.tp.min", 3);

    public static final BoltOption<Integer> CONN_CREATE_TP_MAX_SIZE = valueOf("bolt.conn.create.tp.max", 8);

    public static final BoltOption<Integer> CONN_CREATE_TP_QUEUE_SIZE = valueOf("bolt.conn.create.tp.queue", 50);

    public static final BoltOption<Integer> CONN_CREATE_TP_KEEPALIVE_TIME = valueOf("bolt.conn.create.tp.keepalive", 60);

    private BoltClientOption(String name, T defaultValue) {
        super(name, defaultValue);
    }

}
