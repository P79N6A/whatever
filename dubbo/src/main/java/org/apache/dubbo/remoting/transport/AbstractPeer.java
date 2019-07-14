package org.apache.dubbo.remoting.transport;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.RemotingConstants;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.Endpoint;
import org.apache.dubbo.remoting.RemotingException;

/**
 * 解码器将数据包解析成Request对象后，NettyHandler的messageReceived方法紧接着会收到这个对象，并将这个对象继续向下传递
 * 该对象会被依次传递给NettyServer、MultiMessageHandler、HeartbeatHandler、AllChannelHandler
 * 最后由AllChannelHandler将该对象封装到Runnable实现类对象中，并将Runnable放入线程池中执行后续的调用逻辑
 * 整个调用栈如下：
 * <p>
 * NettyHandler#messageReceived(ChannelHandlerContext, MessageEvent)
 * —> AbstractPeer#received(Channel, Object)
 * —> MultiMessageHandler#received(Channel, Object)
 * —> HeartbeatHandler#received(Channel, Object)
 * —> AllChannelHandler#received(Channel, Object)
 * —> ExecutorService#execute(Runnable)    // 由线程池执行后续的调用逻辑
 */
public abstract class AbstractPeer implements Endpoint, ChannelHandler {

    private final ChannelHandler handler;

    private volatile URL url;

    private volatile boolean closing;

    private volatile boolean closed;

    public AbstractPeer(URL url, ChannelHandler handler) {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler == null");
        }
        this.url = url;
        this.handler = handler;
    }

    @Override
    public void send(Object message) throws RemotingException {
        // 由AbstractClient类实现
        send(message, url.getParameter(RemotingConstants.SENT_KEY, false));
    }

    @Override
    public void close() {
        closed = true;
    }

    @Override
    public void close(int timeout) {
        close();
    }

    @Override
    public void startClose() {
        if (isClosed()) {
            return;
        }
        closing = true;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    protected void setUrl(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        this.url = url;
    }

    @Override
    public ChannelHandler getChannelHandler() {
        if (handler instanceof ChannelHandlerDelegate) {
            return ((ChannelHandlerDelegate) handler).getHandler();
        } else {
            return handler;
        }
    }

    @Deprecated
    public ChannelHandler getHandler() {
        return getDelegateHandler();
    }

    public ChannelHandler getDelegateHandler() {
        return handler;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    public boolean isClosing() {
        return closing && !closed;
    }

    @Override
    public void connected(Channel ch) throws RemotingException {
        if (closed) {
            return;
        }
        handler.connected(ch);
    }

    @Override
    public void disconnected(Channel ch) throws RemotingException {
        handler.disconnected(ch);
    }

    @Override
    public void sent(Channel ch, Object msg) throws RemotingException {
        if (closed) {
            return;
        }
        handler.sent(ch, msg);
    }

    @Override
    public void received(Channel ch, Object msg) throws RemotingException {
        if (closed) {
            return;
        }
        handler.received(ch, msg);
    }

    @Override
    public void caught(Channel ch, Throwable ex) throws RemotingException {
        handler.caught(ch, ex);
    }

}
