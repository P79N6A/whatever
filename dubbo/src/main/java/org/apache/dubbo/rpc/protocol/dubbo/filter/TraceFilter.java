package org.apache.dubbo.rpc.protocol.dubbo.filter;

import com.alibaba.fastjson.JSON;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.constants.RemotingConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.rpc.*;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Activate(group = CommonConstants.PROVIDER)
public class TraceFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(TraceFilter.class);

    private static final String TRACE_MAX = "trace.max";

    private static final String TRACE_COUNT = "trace.count";

    private static final ConcurrentMap<String, Set<Channel>> tracers = new ConcurrentHashMap<>();

    public static void addTracer(Class<?> type, String method, Channel channel, int max) {
        channel.setAttribute(TRACE_MAX, max);
        channel.setAttribute(TRACE_COUNT, new AtomicInteger());
        String key = method != null && method.length() > 0 ? type.getName() + "." + method : type.getName();
        Set<Channel> channels = tracers.get(key);
        if (channels == null) {
            tracers.putIfAbsent(key, new ConcurrentHashSet<>());
            channels = tracers.get(key);
        }
        channels.add(channel);
    }

    public static void removeTracer(Class<?> type, String method, Channel channel) {
        channel.removeAttribute(TRACE_MAX);
        channel.removeAttribute(TRACE_COUNT);
        String key = method != null && method.length() > 0 ? type.getName() + "." + method : type.getName();
        Set<Channel> channels = tracers.get(key);
        if (channels != null) {
            channels.remove(channel);
        }
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        long start = System.currentTimeMillis();
        Result result = invoker.invoke(invocation);
        long end = System.currentTimeMillis();
        if (tracers.size() > 0) {
            String key = invoker.getInterface().getName() + "." + invocation.getMethodName();
            Set<Channel> channels = tracers.get(key);
            if (channels == null || channels.isEmpty()) {
                key = invoker.getInterface().getName();
                channels = tracers.get(key);
            }
            if (CollectionUtils.isNotEmpty(channels)) {
                for (Channel channel : new ArrayList<>(channels)) {
                    if (channel.isConnected()) {
                        try {
                            int max = 1;
                            Integer m = (Integer) channel.getAttribute(TRACE_MAX);
                            if (m != null) {
                                max = m;
                            }
                            int count = 0;
                            AtomicInteger c = (AtomicInteger) channel.getAttribute(TRACE_COUNT);
                            if (c == null) {
                                c = new AtomicInteger();
                                channel.setAttribute(TRACE_COUNT, c);
                            }
                            count = c.getAndIncrement();
                            if (count < max) {
                                String prompt = channel.getUrl().getParameter(RemotingConstants.PROMPT_KEY, RemotingConstants.DEFAULT_PROMPT);
                                channel.send("\r\n" + RpcContext.getContext().getRemoteAddress() + " -> " + invoker.getInterface().getName() + "." + invocation.getMethodName() + "(" + JSON.toJSONString(invocation.getArguments()) + ")" + " -> " + JSON.toJSONString(result.getValue()) + "\r\nelapsed: " + (end - start) + " ms." + "\r\n\r\n" + prompt);
                            }
                            if (count >= max - 1) {
                                channels.remove(channel);
                            }
                        } catch (Throwable e) {
                            channels.remove(channel);
                            logger.warn(e.getMessage(), e);
                        }
                    } else {
                        channels.remove(channel);
                    }
                }
            }
        }
        return result;
    }

}
