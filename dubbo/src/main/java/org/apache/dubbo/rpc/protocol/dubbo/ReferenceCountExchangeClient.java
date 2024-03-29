package org.apache.dubbo.rpc.protocol.dubbo;

import org.apache.dubbo.common.Parameters;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.constants.RemotingConstants;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.ExchangeClient;
import org.apache.dubbo.remoting.exchange.ExchangeHandler;
import org.apache.dubbo.remoting.exchange.ResponseFuture;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.dubbo.common.constants.RpcConstants.LAZY_CONNECT_INITIAL_STATE_KEY;

@SuppressWarnings("deprecation")
final class ReferenceCountExchangeClient implements ExchangeClient {

    private final URL url;

    private final AtomicInteger referenceCount = new AtomicInteger(0);

    private ExchangeClient client;

    public ReferenceCountExchangeClient(ExchangeClient client) {
        this.client = client;
        // 引用计数自增
        referenceCount.incrementAndGet();
        this.url = client.getUrl();
    }

    @Override
    public void reset(URL url) {
        client.reset(url);
    }

    @Override
    public ResponseFuture request(Object request) throws RemotingException {
        // 直接调用被装饰对象的同签名方法
        return client.request(request);
    }

    @Override
    public URL getUrl() {
        return client.getUrl();
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return client.getRemoteAddress();
    }

    @Override
    public ChannelHandler getChannelHandler() {
        return client.getChannelHandler();
    }

    @Override
    public ResponseFuture request(Object request, int timeout) throws RemotingException {
        // 直接调用被装饰对象的同签名方法
        return client.request(request, timeout);
    }

    @Override
    public boolean isConnected() {
        return client.isConnected();
    }

    @Override
    public void reconnect() throws RemotingException {
        client.reconnect();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return client.getLocalAddress();
    }

    @Override
    public boolean hasAttribute(String key) {
        return client.hasAttribute(key);
    }

    @Override
    public void reset(Parameters parameters) {
        client.reset(parameters);
    }

    @Override
    public void send(Object message) throws RemotingException {
        client.send(message);
    }

    @Override
    public ExchangeHandler getExchangeHandler() {
        return client.getExchangeHandler();
    }

    @Override
    public Object getAttribute(String key) {
        return client.getAttribute(key);
    }

    @Override
    public void send(Object message, boolean sent) throws RemotingException {
        client.send(message, sent);
    }

    @Override
    public void setAttribute(String key, Object value) {
        client.setAttribute(key, value);
    }

    @Override
    public void removeAttribute(String key) {
        client.removeAttribute(key);
    }

    @Override
    public void close() {
        close(0);
    }

    @Override
    public void close(int timeout) {
        // referenceCount自减
        if (referenceCount.decrementAndGet() <= 0) {
            if (timeout == 0) {
                client.close();

            } else {
                client.close(timeout);
            }
            replaceWithLazyClient();
        }
    }

    @Override
    public void startClose() {
        client.startClose();
    }

    private void replaceWithLazyClient() {
        URL lazyUrl = URLBuilder.from(url).addParameter(LAZY_CONNECT_INITIAL_STATE_KEY, Boolean.FALSE).addParameter(RemotingConstants.RECONNECT_KEY, Boolean.FALSE).addParameter(RemotingConstants.SEND_RECONNECT_KEY, Boolean.TRUE.toString()).addParameter("warning", Boolean.TRUE.toString()).addParameter(LazyConnectExchangeClient.REQUEST_WITH_WARNING_KEY, true).addParameter("_client_memo", "referencecounthandler.replacewithlazyclient").build();
        if (!(client instanceof LazyConnectExchangeClient) || client.isClosed()) {
            client = new LazyConnectExchangeClient(lazyUrl, client.getExchangeHandler());
        }
    }

    @Override
    public boolean isClosed() {
        return client.isClosed();
    }

    /**
     * 引用计数自增，该方法由外部调用
     */

    public void incrementAndGetCount() {
        // referenceCount自增
        referenceCount.incrementAndGet();
    }

}

