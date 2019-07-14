package org.apache.dubbo.remoting.transport.dispatcher;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;

/**
 * 服务调用过程
 * ChannelEventRunnable#run()
 * —> DecodeHandler#received(Channel, Object)
 * —> HeaderExchangeHandler#received(Channel, Object)
 * —> HeaderExchangeHandler#handleRequest(ExchangeChannel, Request)
 * —> DubboProtocol.requestHandler#reply(ExchangeChannel, Object)
 * —> Filter#invoke(Invoker, Invocation)
 * —> AbstractProxyInvoker#invoke(Invocation)
 * —> Wrapper0#invokeMethod(Object, String, Class[], Object[])
 * —> DemoServiceImpl#sayHello(String)
 */
public class ChannelEventRunnable implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ChannelEventRunnable.class);

    private final ChannelHandler handler;

    private final Channel channel;

    private final ChannelState state;

    private final Throwable exception;

    private final Object message;

    public ChannelEventRunnable(Channel channel, ChannelHandler handler, ChannelState state) {
        this(channel, handler, state, null);
    }

    public ChannelEventRunnable(Channel channel, ChannelHandler handler, ChannelState state, Object message) {
        this(channel, handler, state, message, null);
    }

    public ChannelEventRunnable(Channel channel, ChannelHandler handler, ChannelState state, Throwable t) {
        this(channel, handler, state, null, t);
    }

    public ChannelEventRunnable(Channel channel, ChannelHandler handler, ChannelState state, Object message, Throwable exception) {
        this.channel = channel;
        this.handler = handler;
        // 对应netty的事件
        this.state = state;
        this.message = message;
        this.exception = exception;
    }

    /**
     * ChannelEventRunnable仅是一个中转站，将参数传给其他ChannelHandler对象处理，该对象类型为DecodeHandler
     */
    @Override
    public void run() {
        // 检测通道状态，对于请求或响应消息，state = RECEIVED
        if (state == ChannelState.RECEIVED) {
            try {
                // 将channel和message传给ChannelHandler对象，进行后续的调用
                handler.received(channel, message);
            } catch (Exception e) {
                logger.warn("ChannelEventRunnable handle " + state + " operation error, channel is " + channel + ", message is " + message, e);
            }
        }
        // 其他消息类型
        else {
            switch (state) {
                case CONNECTED:
                    try {
                        handler.connected(channel);
                    } catch (Exception e) {
                        logger.warn("ChannelEventRunnable handle " + state + " operation error, channel is " + channel, e);
                    }
                    break;
                case DISCONNECTED:
                    try {
                        handler.disconnected(channel);
                    } catch (Exception e) {
                        logger.warn("ChannelEventRunnable handle " + state + " operation error, channel is " + channel, e);
                    }
                    break;
                case SENT:
                    try {
                        handler.sent(channel, message);
                    } catch (Exception e) {
                        logger.warn("ChannelEventRunnable handle " + state + " operation error, channel is " + channel + ", message is " + message, e);
                    }
                    break;
                case CAUGHT:
                    try {
                        handler.caught(channel, exception);
                    } catch (Exception e) {
                        logger.warn("ChannelEventRunnable handle " + state + " operation error, channel is " + channel + ", message is: " + message + ", exception is " + exception, e);
                    }
                    break;
                default:
                    logger.warn("unknown state: " + state + ", message is " + message);
            }
        }

    }

    public enum ChannelState {

        CONNECTED,

        DISCONNECTED,

        SENT,

        RECEIVED,

        CAUGHT
    }

}
