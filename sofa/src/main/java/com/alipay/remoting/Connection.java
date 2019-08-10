package com.alipay.remoting;

import com.alipay.remoting.log.BoltLoggerFactory;
import com.alipay.remoting.rpc.protocol.RpcProtocolV2;
import com.alipay.remoting.util.ConcurrentHashSet;
import com.alipay.remoting.util.RemotingUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Connection {

    private static final Logger logger = BoltLoggerFactory.getLogger("CommonDefault");

    private Channel channel;

    private final ConcurrentHashMap<Integer, InvokeFuture> invokeFutureMap = new ConcurrentHashMap<Integer, InvokeFuture>(4);

    public static final AttributeKey<Connection> CONNECTION = AttributeKey.valueOf("connection");

    public static final AttributeKey<Integer> HEARTBEAT_COUNT = AttributeKey.valueOf("heartbeatCount");

    public static final AttributeKey<Boolean> HEARTBEAT_SWITCH = AttributeKey.valueOf("heartbeatSwitch");

    public static final AttributeKey<ProtocolCode> PROTOCOL = AttributeKey.valueOf("protocol");

    private ProtocolCode protocolCode;

    public static final AttributeKey<Byte> VERSION = AttributeKey.valueOf("version");

    private byte version = RpcProtocolV2.PROTOCOL_VERSION_1;

    private Url url;

    private final ConcurrentHashMap<Integer, String> id2PoolKey = new ConcurrentHashMap<Integer, String>(256);

    private Set<String> poolKeys = new ConcurrentHashSet<String>();

    private AtomicBoolean closed = new AtomicBoolean(false);

    private final ConcurrentHashMap<String, Object> attributes = new ConcurrentHashMap<String, Object>();

    private final AtomicInteger referenceCount = new AtomicInteger();

    private static final int NO_REFERENCE = 0;

    public Connection(Channel channel) {
        this.channel = channel;
        this.channel.attr(CONNECTION).set(this);
    }

    public Connection(Channel channel, Url url) {
        this(channel);
        this.url = url;
        this.poolKeys.add(url.getUniqueKey());
    }

    public Connection(Channel channel, ProtocolCode protocolCode, Url url) {
        this(channel, url);
        this.protocolCode = protocolCode;
        this.init();
    }

    public Connection(Channel channel, ProtocolCode protocolCode, byte version, Url url) {
        this(channel, url);
        this.protocolCode = protocolCode;
        this.version = version;
        this.init();
    }

    private void init() {
        this.channel.attr(HEARTBEAT_COUNT).set(0);
        this.channel.attr(PROTOCOL).set(this.protocolCode);
        this.channel.attr(VERSION).set(this.version);
        this.channel.attr(HEARTBEAT_SWITCH).set(true);
    }

    public boolean isFine() {
        return this.channel != null && this.channel.isActive();
    }

    public void increaseRef() {
        this.referenceCount.getAndIncrement();
    }

    public void decreaseRef() {
        this.referenceCount.getAndDecrement();
    }

    public boolean noRef() {
        return this.referenceCount.get() == NO_REFERENCE;
    }

    public InetSocketAddress getRemoteAddress() {
        return (InetSocketAddress) this.channel.remoteAddress();
    }

    public String getRemoteIP() {
        return RemotingUtil.parseRemoteIP(this.channel);
    }

    public int getRemotePort() {
        return RemotingUtil.parseRemotePort(this.channel);
    }

    public InetSocketAddress getLocalAddress() {
        return (InetSocketAddress) this.channel.localAddress();
    }

    public String getLocalIP() {
        return RemotingUtil.parseLocalIP(this.channel);
    }

    public int getLocalPort() {
        return RemotingUtil.parseLocalPort(this.channel);
    }

    public Channel getChannel() {
        return this.channel;
    }

    public InvokeFuture getInvokeFuture(int id) {
        return this.invokeFutureMap.get(id);
    }

    public InvokeFuture addInvokeFuture(InvokeFuture future) {
        return this.invokeFutureMap.putIfAbsent(future.invokeId(), future);
    }

    public InvokeFuture removeInvokeFuture(int id) {
        return this.invokeFutureMap.remove(id);
    }

    public void onClose() {
        Iterator<Entry<Integer, InvokeFuture>> iter = invokeFutureMap.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<Integer, InvokeFuture> entry = iter.next();
            iter.remove();
            InvokeFuture future = entry.getValue();
            if (future != null) {
                future.putResponse(future.createConnectionClosedResponse(this.getRemoteAddress()));
                future.cancelTimeout();
                future.tryAsyncExecuteInvokeCallbackAbnormally();
            }
        }
    }

    public void close() {
        if (closed.compareAndSet(false, true)) {
            try {
                if (this.getChannel() != null) {
                    this.getChannel().close().addListener(new ChannelFutureListener() {

                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            if (logger.isInfoEnabled()) {
                                logger.info("Close the connection to remote address={}, result={}, cause={}", RemotingUtil.parseRemoteAddress(Connection.this.getChannel()), future.isSuccess(), future.cause());
                            }
                        }

                    });
                }
            } catch (Exception e) {
                logger.warn("Exception caught when closing connection {}", RemotingUtil.parseRemoteAddress(Connection.this.getChannel()), e);
            }
        }
    }

    public boolean isInvokeFutureMapFinish() {
        return invokeFutureMap.isEmpty();
    }

    public void addPoolKey(String poolKey) {
        poolKeys.add(poolKey);
    }

    public Set<String> getPoolKeys() {
        return new HashSet<String>(poolKeys);
    }

    public void removePoolKey(String poolKey) {
        poolKeys.remove(poolKey);
    }

    public Url getUrl() {
        return url;
    }

    public void addIdPoolKeyMapping(Integer id, String poolKey) {
        this.id2PoolKey.put(id, poolKey);
    }

    public String removeIdPoolKeyMapping(Integer id) {
        return this.id2PoolKey.remove(id);
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public Object setAttributeIfAbsent(String key, Object value) {
        return attributes.putIfAbsent(key, value);
    }

    public void removeAttribute(String key) {
        attributes.remove(key);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public void clearAttributes() {
        attributes.clear();
    }

    public ConcurrentHashMap<Integer, InvokeFuture> getInvokeFutureMap() {
        return invokeFutureMap;
    }

}
