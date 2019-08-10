package com.alipay.remoting.rpc;

import com.alipay.remoting.*;
import com.alipay.remoting.codec.Codec;
import com.alipay.remoting.config.ConfigManager;
import com.alipay.remoting.config.switches.GlobalSwitch;
import com.alipay.remoting.exception.RemotingException;
import com.alipay.remoting.log.BoltLoggerFactory;
import com.alipay.remoting.rpc.protocol.UserProcessor;
import com.alipay.remoting.rpc.protocol.UserProcessorRegisterHelper;
import com.alipay.remoting.util.NettyEventLoopUtil;
import com.alipay.remoting.util.RemotingUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class RpcServer extends AbstractRemotingServer {

    private static final Logger logger = BoltLoggerFactory.getLogger("RpcRemoting");

    private ServerBootstrap bootstrap;

    private ChannelFuture channelFuture;

    private ConnectionEventHandler connectionEventHandler;

    private ConnectionEventListener connectionEventListener = new ConnectionEventListener();

    private ConcurrentHashMap<String, UserProcessor<?>> userProcessors = new ConcurrentHashMap<String, UserProcessor<?>>(4);

    private final EventLoopGroup bossGroup = NettyEventLoopUtil.newEventLoopGroup(1, new NamedThreadFactory("Rpc-netty-server-boss", false));

    private static final EventLoopGroup workerGroup = NettyEventLoopUtil.newEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2, new NamedThreadFactory("Rpc-netty-server-worker", true));

    private RemotingAddressParser addressParser;

    private DefaultServerConnectionManager connectionManager;

    protected RpcRemoting rpcRemoting;

    private Codec codec = new RpcCodec();

    static {
        if (workerGroup instanceof NioEventLoopGroup) {
            ((NioEventLoopGroup) workerGroup).setIoRatio(ConfigManager.netty_io_ratio());
        } else if (workerGroup instanceof EpollEventLoopGroup) {
            ((EpollEventLoopGroup) workerGroup).setIoRatio(ConfigManager.netty_io_ratio());
        }
    }

    public RpcServer(int port) {
        this(port, false);
    }

    public RpcServer(String ip, int port) {
        this(ip, port, false);
    }

    public RpcServer(int port, boolean manageConnection) {
        super(port);
        if (manageConnection) {
            this.switches().turnOn(GlobalSwitch.SERVER_MANAGE_CONNECTION_SWITCH);
        }
    }

    public RpcServer(String ip, int port, boolean manageConnection) {
        super(ip, port);
        if (manageConnection) {
            this.switches().turnOn(GlobalSwitch.SERVER_MANAGE_CONNECTION_SWITCH);
        }
    }

    public RpcServer(int port, boolean manageConnection, boolean syncStop) {
        this(port, manageConnection);
        if (syncStop) {
            this.switches().turnOn(GlobalSwitch.SERVER_SYNC_STOP);
        }
    }

    @Override
    protected void doInit() {
        if (this.addressParser == null) {
            this.addressParser = new RpcAddressParser();
        }
        if (this.switches().isOn(GlobalSwitch.SERVER_MANAGE_CONNECTION_SWITCH)) {
            ConnectionSelectStrategy connectionSelectStrategy = new RandomSelectStrategy(null);
            this.connectionManager = new DefaultServerConnectionManager(connectionSelectStrategy);
            this.connectionManager.startup();
            this.connectionEventHandler = new RpcConnectionEventHandler(switches());
            this.connectionEventHandler.setConnectionManager(this.connectionManager);
            this.connectionEventHandler.setConnectionEventListener(this.connectionEventListener);
        } else {
            this.connectionEventHandler = new ConnectionEventHandler(switches());
            this.connectionEventHandler.setConnectionEventListener(this.connectionEventListener);
        }
        initRpcRemoting();
        this.bootstrap = new ServerBootstrap();
        this.bootstrap.group(bossGroup, workerGroup).channel(NettyEventLoopUtil.getServerSocketChannelClass()).option(ChannelOption.SO_BACKLOG, ConfigManager.tcp_so_backlog()).option(ChannelOption.SO_REUSEADDR, ConfigManager.tcp_so_reuseaddr()).childOption(ChannelOption.TCP_NODELAY, ConfigManager.tcp_nodelay()).childOption(ChannelOption.SO_KEEPALIVE, ConfigManager.tcp_so_keepalive());
        initWriteBufferWaterMark();
        if (ConfigManager.netty_buffer_pooled()) {
            this.bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT).childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        } else {
            this.bootstrap.option(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT).childOption(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT);
        }
        NettyEventLoopUtil.enableTriggeredMode(bootstrap);
        final boolean idleSwitch = ConfigManager.tcp_idle_switch();
        final int idleTime = ConfigManager.tcp_server_idle();
        final ChannelHandler serverIdleHandler = new ServerIdleHandler();
        final RpcHandler rpcHandler = new RpcHandler(true, this.userProcessors);
        this.bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(SocketChannel channel) {
                ChannelPipeline pipeline = channel.pipeline();
                pipeline.addLast("decoder", codec.newDecoder());
                pipeline.addLast("encoder", codec.newEncoder());
                if (idleSwitch) {
                    pipeline.addLast("idleStateHandler", new IdleStateHandler(0, 0, idleTime, TimeUnit.MILLISECONDS));
                    pipeline.addLast("serverIdleHandler", serverIdleHandler);
                }
                pipeline.addLast("connectionEventHandler", connectionEventHandler);
                pipeline.addLast("handler", rpcHandler);
                createConnection(channel);
            }

            private void createConnection(SocketChannel channel) {
                Url url = addressParser.parse(RemotingUtil.parseRemoteAddress(channel));
                if (switches().isOn(GlobalSwitch.SERVER_MANAGE_CONNECTION_SWITCH)) {
                    connectionManager.add(new Connection(channel, url), url.getUniqueKey());
                } else {
                    new Connection(channel, url);
                }
                channel.pipeline().fireUserEventTriggered(ConnectionEventType.CONNECT);
            }
        });
    }

    @Override
    protected boolean doStart() throws InterruptedException {
        this.channelFuture = this.bootstrap.bind(new InetSocketAddress(ip(), port())).sync();
        return this.channelFuture.isSuccess();
    }

    @Override
    protected boolean doStop() {
        if (null != this.channelFuture) {
            this.channelFuture.channel().close();
        }
        if (this.switches().isOn(GlobalSwitch.SERVER_SYNC_STOP)) {
            this.bossGroup.shutdownGracefully().awaitUninterruptibly();
        } else {
            this.bossGroup.shutdownGracefully();
        }
        if (this.switches().isOn(GlobalSwitch.SERVER_MANAGE_CONNECTION_SWITCH) && null != this.connectionManager) {
            this.connectionManager.shutdown();
            logger.warn("Close all connections from server side!");
        }
        logger.warn("Rpc Server stopped!");
        return true;
    }

    protected void initRpcRemoting() {
        this.rpcRemoting = new RpcServerRemoting(new RpcCommandFactory(), this.addressParser, this.connectionManager);
    }

    @Override
    public void registerProcessor(byte protocolCode, CommandCode cmd, RemotingProcessor<?> processor) {
        ProtocolManager.getProtocol(ProtocolCode.fromBytes(protocolCode)).getCommandHandler().registerProcessor(cmd, processor);
    }

    @Override
    public void registerDefaultExecutor(byte protocolCode, ExecutorService executor) {
        ProtocolManager.getProtocol(ProtocolCode.fromBytes(protocolCode)).getCommandHandler().registerDefaultExecutor(executor);
    }

    public void addConnectionEventProcessor(ConnectionEventType type, ConnectionEventProcessor processor) {
        this.connectionEventListener.addConnectionEventProcessor(type, processor);
    }

    @Override
    public void registerUserProcessor(UserProcessor<?> processor) {
        UserProcessorRegisterHelper.registerUserProcessor(processor, this.userProcessors);
    }

    public void oneway(final String addr, final Object request) throws RemotingException, InterruptedException {
        check();
        this.rpcRemoting.oneway(addr, request, null);
    }

    public void oneway(final String addr, final Object request, final InvokeContext invokeContext) throws RemotingException, InterruptedException {
        check();
        this.rpcRemoting.oneway(addr, request, invokeContext);
    }

    public void oneway(final Url url, final Object request) throws RemotingException, InterruptedException {
        check();
        this.rpcRemoting.oneway(url, request, null);
    }

    public void oneway(final Url url, final Object request, final InvokeContext invokeContext) throws RemotingException, InterruptedException {
        check();
        this.rpcRemoting.oneway(url, request, invokeContext);
    }

    public void oneway(final Connection conn, final Object request) throws RemotingException {
        this.rpcRemoting.oneway(conn, request, null);
    }

    public void oneway(final Connection conn, final Object request, final InvokeContext invokeContext) throws RemotingException {
        this.rpcRemoting.oneway(conn, request, invokeContext);
    }

    public Object invokeSync(final String addr, final Object request, final int timeoutMillis) throws RemotingException, InterruptedException {
        check();
        return this.rpcRemoting.invokeSync(addr, request, null, timeoutMillis);
    }

    public Object invokeSync(final String addr, final Object request, final InvokeContext invokeContext, final int timeoutMillis) throws RemotingException, InterruptedException {
        check();
        return this.rpcRemoting.invokeSync(addr, request, invokeContext, timeoutMillis);
    }

    public Object invokeSync(Url url, Object request, int timeoutMillis) throws RemotingException, InterruptedException {
        check();
        return this.rpcRemoting.invokeSync(url, request, null, timeoutMillis);
    }

    public Object invokeSync(final Url url, final Object request, final InvokeContext invokeContext, final int timeoutMillis) throws RemotingException, InterruptedException {
        check();
        return this.rpcRemoting.invokeSync(url, request, invokeContext, timeoutMillis);
    }

    public Object invokeSync(final Connection conn, final Object request, final int timeoutMillis) throws RemotingException, InterruptedException {
        return this.rpcRemoting.invokeSync(conn, request, null, timeoutMillis);
    }

    public Object invokeSync(final Connection conn, final Object request, final InvokeContext invokeContext, final int timeoutMillis) throws RemotingException, InterruptedException {
        return this.rpcRemoting.invokeSync(conn, request, invokeContext, timeoutMillis);
    }

    public RpcResponseFuture invokeWithFuture(final String addr, final Object request, final int timeoutMillis) throws RemotingException, InterruptedException {
        check();
        return this.rpcRemoting.invokeWithFuture(addr, request, null, timeoutMillis);
    }

    public RpcResponseFuture invokeWithFuture(final String addr, final Object request, final InvokeContext invokeContext, final int timeoutMillis) throws RemotingException, InterruptedException {
        check();
        return this.rpcRemoting.invokeWithFuture(addr, request, invokeContext, timeoutMillis);
    }

    public RpcResponseFuture invokeWithFuture(final Url url, final Object request, final int timeoutMillis) throws RemotingException, InterruptedException {
        check();
        return this.rpcRemoting.invokeWithFuture(url, request, null, timeoutMillis);
    }

    public RpcResponseFuture invokeWithFuture(final Url url, final Object request, final InvokeContext invokeContext, final int timeoutMillis) throws RemotingException, InterruptedException {
        check();
        return this.rpcRemoting.invokeWithFuture(url, request, invokeContext, timeoutMillis);
    }

    public RpcResponseFuture invokeWithFuture(final Connection conn, final Object request, final int timeoutMillis) throws RemotingException {
        return this.rpcRemoting.invokeWithFuture(conn, request, null, timeoutMillis);
    }

    public RpcResponseFuture invokeWithFuture(final Connection conn, final Object request, final InvokeContext invokeContext, final int timeoutMillis) throws RemotingException {
        return this.rpcRemoting.invokeWithFuture(conn, request, invokeContext, timeoutMillis);
    }

    public void invokeWithCallback(final String addr, final Object request, final InvokeCallback invokeCallback, final int timeoutMillis) throws RemotingException, InterruptedException {
        check();
        this.rpcRemoting.invokeWithCallback(addr, request, null, invokeCallback, timeoutMillis);
    }

    public void invokeWithCallback(final String addr, final Object request, final InvokeContext invokeContext, final InvokeCallback invokeCallback, final int timeoutMillis) throws RemotingException, InterruptedException {
        check();
        this.rpcRemoting.invokeWithCallback(addr, request, invokeContext, invokeCallback, timeoutMillis);
    }

    public void invokeWithCallback(final Url url, final Object request, final InvokeCallback invokeCallback, final int timeoutMillis) throws RemotingException, InterruptedException {
        check();
        this.rpcRemoting.invokeWithCallback(url, request, null, invokeCallback, timeoutMillis);
    }

    public void invokeWithCallback(final Url url, final Object request, final InvokeContext invokeContext, final InvokeCallback invokeCallback, final int timeoutMillis) throws RemotingException, InterruptedException {
        check();
        this.rpcRemoting.invokeWithCallback(url, request, invokeContext, invokeCallback, timeoutMillis);
    }

    public void invokeWithCallback(final Connection conn, final Object request, final InvokeCallback invokeCallback, final int timeoutMillis) throws RemotingException {
        this.rpcRemoting.invokeWithCallback(conn, request, null, invokeCallback, timeoutMillis);
    }

    public void invokeWithCallback(final Connection conn, final Object request, final InvokeContext invokeContext, final InvokeCallback invokeCallback, final int timeoutMillis) throws RemotingException {
        this.rpcRemoting.invokeWithCallback(conn, request, invokeContext, invokeCallback, timeoutMillis);
    }

    public boolean isConnected(String remoteAddr) {
        Url url = this.rpcRemoting.addressParser.parse(remoteAddr);
        return this.isConnected(url);
    }

    public boolean isConnected(Url url) {
        Connection conn = this.rpcRemoting.connectionManager.get(url.getUniqueKey());
        if (null != conn) {
            return conn.isFine();
        }
        return false;
    }

    private void check() {
        if (!this.switches().isOn(GlobalSwitch.SERVER_MANAGE_CONNECTION_SWITCH)) {
            throw new UnsupportedOperationException("Please enable connection manage feature of Rpc Server before call this method! See comments in constructor RpcServer(int port, boolean manageConnection) to find how to enable!");
        }
    }

    private void initWriteBufferWaterMark() {
        int lowWaterMark = this.netty_buffer_low_watermark();
        int highWaterMark = this.netty_buffer_high_watermark();
        if (lowWaterMark > highWaterMark) {
            throw new IllegalArgumentException(String.format("[server side] bolt netty high water mark {%s} should not be smaller than low water mark {%s} bytes)", highWaterMark, lowWaterMark));
        } else {
            logger.warn("[server side] bolt netty low water mark is {} bytes, high water mark is {} bytes", lowWaterMark, highWaterMark);
        }
        this.bootstrap.childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(lowWaterMark, highWaterMark));
    }

    public RemotingAddressParser getAddressParser() {
        return this.addressParser;
    }

    public void setAddressParser(RemotingAddressParser addressParser) {
        this.addressParser = addressParser;
    }

    public DefaultConnectionManager getConnectionManager() {
        return connectionManager;
    }

}
