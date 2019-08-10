package com.alipay.remoting.config;

public class BoltServerOption<T> extends BoltGenericOption<T> {

    public static final BoltOption<Integer> TCP_SO_BACKLOG = valueOf("bolt.tcp.so.backlog", 1024);

    public static final BoltOption<Boolean> NETTY_EPOLL_LT = valueOf("bolt.netty.epoll.lt", true);

    public static final BoltOption<Integer> TCP_SERVER_IDLE = valueOf("bolt.tcp.server.idle.interval", 90 * 1000);

    private BoltServerOption(String name, T defaultValue) {
        super(name, defaultValue);
    }

}
