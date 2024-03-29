package com.alipay.remoting.config;

import com.alipay.remoting.serialization.SerializerManager;

public class Configs {

    public static final String TCP_NODELAY = "bolt.tcp.nodelay";

    public static final String TCP_NODELAY_DEFAULT = "true";

    public static final String TCP_SO_REUSEADDR = "bolt.tcp.so.reuseaddr";

    public static final String TCP_SO_REUSEADDR_DEFAULT = "true";

    public static final String TCP_SO_BACKLOG = "bolt.tcp.so.backlog";

    public static final String TCP_SO_BACKLOG_DEFAULT = "1024";

    public static final String TCP_SO_KEEPALIVE = "bolt.tcp.so.keepalive";

    public static final String TCP_SO_KEEPALIVE_DEFAULT = "true";

    public static final String NETTY_IO_RATIO = "bolt.netty.io.ratio";

    public static final String NETTY_IO_RATIO_DEFAULT = "70";

    public static final String NETTY_BUFFER_POOLED = "bolt.netty.buffer.pooled";

    public static final String NETTY_BUFFER_POOLED_DEFAULT = "true";

    public static final String NETTY_BUFFER_HIGH_WATERMARK = "bolt.netty.buffer.high.watermark";

    public static final String NETTY_BUFFER_HIGH_WATERMARK_DEFAULT = Integer.toString(64 * 1024);

    public static final String NETTY_BUFFER_LOW_WATERMARK = "bolt.netty.buffer.low.watermark";

    public static final String NETTY_BUFFER_LOW_WATERMARK_DEFAULT = Integer.toString(32 * 1024);

    public static final String NETTY_EPOLL_SWITCH = "bolt.netty.epoll.switch";

    public static final String NETTY_EPOLL_SWITCH_DEFAULT = "true";

    public static final String NETTY_EPOLL_LT = "bolt.netty.epoll.lt";

    public static final String NETTY_EPOLL_LT_DEFAULT = "true";

    public static final String TCP_IDLE_SWITCH = "bolt.tcp.heartbeat.switch";

    public static final String TCP_IDLE_SWITCH_DEFAULT = "true";

    public static final String TCP_IDLE = "bolt.tcp.heartbeat.interval";

    public static final String TCP_IDLE_DEFAULT = "15000";

    public static final String TCP_IDLE_MAXTIMES = "bolt.tcp.heartbeat.maxtimes";

    public static final String TCP_IDLE_MAXTIMES_DEFAULT = "3";

    public static final String TCP_SERVER_IDLE = "bolt.tcp.server.idle.interval";

    public static final String TCP_SERVER_IDLE_DEFAULT = "90000";

    public static final String CONN_CREATE_TP_MIN_SIZE = "bolt.conn.create.tp.min";

    public static final String CONN_CREATE_TP_MIN_SIZE_DEFAULT = "3";

    public static final String CONN_CREATE_TP_MAX_SIZE = "bolt.conn.create.tp.max";

    public static final String CONN_CREATE_TP_MAX_SIZE_DEFAULT = "8";

    public static final String CONN_CREATE_TP_QUEUE_SIZE = "bolt.conn.create.tp.queue";

    public static final String CONN_CREATE_TP_QUEUE_SIZE_DEFAULT = "50";

    public static final String CONN_CREATE_TP_KEEPALIVE_TIME = "bolt.conn.create.tp.keepalive";

    public static final String CONN_CREATE_TP_KEEPALIVE_TIME_DEFAULT = "60";

    public static final int DEFAULT_CONNECT_TIMEOUT = 1000;

    public static final int DEFAULT_CONN_NUM_PER_URL = 1;

    public static final int MAX_CONN_NUM_PER_URL = 100 * 10000;

    public static final String TP_MIN_SIZE = "bolt.tp.min";

    public static final String TP_MIN_SIZE_DEFAULT = "20";

    public static final String TP_MAX_SIZE = "bolt.tp.max";

    public static final String TP_MAX_SIZE_DEFAULT = "400";

    public static final String TP_QUEUE_SIZE = "bolt.tp.queue";

    public static final String TP_QUEUE_SIZE_DEFAULT = "600";

    public static final String TP_KEEPALIVE_TIME = "bolt.tp.keepalive";

    public static final String TP_KEEPALIVE_TIME_DEFAULT = "60";

    public static final String CONN_RECONNECT_SWITCH = "bolt.conn.reconnect.switch";

    public static final String CONN_RECONNECT_SWITCH_DEFAULT = "false";

    public static final String CONN_MONITOR_SWITCH = "bolt.conn.monitor.switch";

    public static final String CONN_MONITOR_SWITCH_DEFAULT = "false";

    public static final String CONN_MONITOR_INITIAL_DELAY = "bolt.conn.monitor.initial.delay";

    public static final String CONN_MONITOR_INITIAL_DELAY_DEFAULT = "10000";

    public static final String CONN_MONITOR_PERIOD = "bolt.conn.monitor.period";

    public static final String CONN_MONITOR_PERIOD_DEFAULT = "180000";

    public static final String CONN_THRESHOLD = "bolt.conn.threshold";

    public static final String CONN_THRESHOLD_DEFAULT = "3";

    @Deprecated
    public static final String RETRY_DETECT_PERIOD = "bolt.retry.delete.period";

    public static final String RETRY_DETECT_PERIOD_DEFAULT = "5000";

    public static final String CONN_SERVICE_STATUS = "bolt.conn.service.status";

    public static final String CONN_SERVICE_STATUS_OFF = "off";

    public static final String CONN_SERVICE_STATUS_ON = "on";

    public static final String SERIALIZER = "bolt.serializer";

    public static final String SERIALIZER_DEFAULT = String.valueOf(SerializerManager.Hessian2);

    public static final String DEFAULT_CHARSET = "UTF-8";

}