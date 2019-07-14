package org.apache.dubbo.remoting.transport;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.Decodeable;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.Response;

/**
 * 请求解码可在IO线程上执行，也可在线程池中执行，取决于运行时配置
 * DecodeHandler的意义是保证请求或响应对象可在线程池中被解码
 * 解码完毕后，完全解码后的Request对象会继续向后传递，下一站是HeaderExchangeHandler
 */
public class DecodeHandler extends AbstractChannelHandlerDelegate {

    private static final Logger log = LoggerFactory.getLogger(DecodeHandler.class);

    public DecodeHandler(ChannelHandler handler) {
        super(handler);
    }

    @Override
    public void received(Channel channel, Object message) throws RemotingException {
        if (message instanceof Decodeable) {
            // 对Decodeable接口实现类对象进行解码
            decode(message);
        }
        if (message instanceof Request) {
            // 对Request的data字段进行解码
            decode(((Request) message).getData());
        }
        if (message instanceof Response) {
            // 对Request的result字段进行解码
            decode(((Response) message).getResult());
        }
        // 执行后续逻辑
        handler.received(channel, message);
    }

    private void decode(Object message) {
        // Decodeable接口有两个实现类，分别为DecodeableRpcInvocation和DecodeableRpcResult
        if (message instanceof Decodeable) {
            try {
                // 执行解码逻辑
                ((Decodeable) message).decode();
                if (log.isDebugEnabled()) {
                    // Decode decodeable message org.apache.dubbo.rpc.protocol.dubbo.DecodeableRpcInvocation
                    log.debug("Decode decodeable message " + message.getClass().getName());
                }
            } catch (Throwable e) {
                if (log.isWarnEnabled()) {
                    log.warn("Call Decodeable.decode failed: " + e.getMessage(), e);
                }
            }
        }
    }

}
