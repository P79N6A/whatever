package com.alipay.remoting;

import com.alipay.remoting.config.switches.GlobalSwitch;
import com.alipay.remoting.log.BoltLoggerFactory;
import com.alipay.remoting.util.RemotingUtil;
import com.alipay.remoting.util.StringUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.Attribute;
import org.slf4j.Logger;

import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Sharable
public class ConnectionEventHandler extends ChannelDuplexHandler {
    private static final Logger logger = BoltLoggerFactory.getLogger("ConnectionEvent");

    private ConnectionManager connectionManager;

    private ConnectionEventListener eventListener;

    private ConnectionEventExecutor eventExecutor;

    private Reconnector reconnectManager;

    private GlobalSwitch globalSwitch;

    public ConnectionEventHandler() {
    }

    public ConnectionEventHandler(GlobalSwitch globalSwitch) {
        this.globalSwitch = globalSwitch;
    }

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
        if (logger.isInfoEnabled()) {
            final String local = localAddress == null ? null : RemotingUtil.parseSocketAddressToString(localAddress);
            final String remote = remoteAddress == null ? "UNKNOWN" : RemotingUtil.parseSocketAddressToString(remoteAddress);
            if (local == null) {
                if (logger.isInfoEnabled()) {
                    logger.info("Try connect to {}", remote);
                }
            } else {
                if (logger.isInfoEnabled()) {
                    logger.info("Try connect from {} to {}", local, remote);
                }
            }
        }
        super.connect(ctx, remoteAddress, localAddress, promise);
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        infoLog("Connection disconnect to {}", RemotingUtil.parseRemoteAddress(ctx.channel()));
        super.disconnect(ctx, promise);
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        infoLog("Connection closed: {}", RemotingUtil.parseRemoteAddress(ctx.channel()));
        final Connection conn = ctx.channel().attr(Connection.CONNECTION).get();
        if (conn != null) {
            conn.onClose();
        }
        super.close(ctx, promise);
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        infoLog("Connection channel registered: {}", RemotingUtil.parseRemoteAddress(ctx.channel()));
        super.channelRegistered(ctx);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        infoLog("Connection channel unregistered: {}", RemotingUtil.parseRemoteAddress(ctx.channel()));
        super.channelUnregistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        infoLog("Connection channel active: {}", RemotingUtil.parseRemoteAddress(ctx.channel()));
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String remoteAddress = RemotingUtil.parseRemoteAddress(ctx.channel());
        infoLog("Connection channel inactive: {}", remoteAddress);
        super.channelInactive(ctx);
        Attribute attr = ctx.channel().attr(Connection.CONNECTION);
        if (null != attr) {
            if (this.globalSwitch != null && this.globalSwitch.isOn(GlobalSwitch.CONN_RECONNECT_SWITCH)) {
                Connection conn = (Connection) attr.get();
                if (reconnectManager != null) {
                    reconnectManager.reconnect(conn.getUrl());
                }
            }
            onEvent((Connection) attr.get(), remoteAddress, ConnectionEventType.CLOSE);
        }
    }

    /**
     * 用户自定义的事件
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object event) throws Exception {
        if (event instanceof ConnectionEventType) {
            switch ((ConnectionEventType) event) {
                case CONNECT:
                    Channel channel = ctx.channel();
                    if (null != channel) {
                        Connection connection = channel.attr(Connection.CONNECTION).get();
                        this.onEvent(connection, connection.getUrl().getOriginUrl(), ConnectionEventType.CONNECT);
                    } else {
                        logger.warn("channel null when handle user triggered event in ConnectionEventHandler!");
                    }
                    break;
                default:
                    break;
            }
        } else {
            super.userEventTriggered(ctx, event);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        final String remoteAddress = RemotingUtil.parseRemoteAddress(ctx.channel());
        final String localAddress = RemotingUtil.parseLocalAddress(ctx.channel());
        logger.warn("ExceptionCaught in connection: local[{}], remote[{}], close the connection! Cause[{}:{}]", localAddress, remoteAddress, cause.getClass().getSimpleName(), cause.getMessage());
        ctx.channel().close();
    }

    private void onEvent(final Connection conn, final String remoteAddress, final ConnectionEventType type) {
        if (this.eventListener != null) {
            // 提交给线程池执行
            this.eventExecutor.onEvent(new Runnable() {
                @Override
                public void run() {
                    ConnectionEventHandler.this.eventListener.onEvent(type, remoteAddress, conn);
                }
            });
        }
    }

    public ConnectionEventListener getConnectionEventListener() {
        return eventListener;
    }

    public void setConnectionEventListener(ConnectionEventListener listener) {
        if (listener != null) {
            this.eventListener = listener;
            if (this.eventExecutor == null) {
                this.eventExecutor = new ConnectionEventExecutor();
            }
        }
    }

    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public void setConnectionManager(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Deprecated
    public void setReconnectManager(ReconnectManager reconnectManager) {
        this.reconnectManager = reconnectManager;
    }

    public void setReconnector(Reconnector reconnector) {
        this.reconnectManager = reconnector;
    }

    /**
     * 连接事件处理线程池
     */
    public class ConnectionEventExecutor {
        Logger logger = BoltLoggerFactory.getLogger("CommonDefault");

        ExecutorService executor = new ThreadPoolExecutor(1, 1, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(10000), new NamedThreadFactory("Bolt-conn-event-executor", true));

        public void onEvent(Runnable runnable) {
            try {
                executor.execute(runnable);
            } catch (Throwable t) {
                logger.error("Exception caught when execute connection event!", t);
            }
        }

    }

    private void infoLog(String format, String addr) {
        if (logger.isInfoEnabled()) {
            if (StringUtils.isNotEmpty(addr)) {
                logger.info(format, addr);
            } else {
                logger.info(format, "UNKNOWN-ADDR");
            }
        }
    }

}
