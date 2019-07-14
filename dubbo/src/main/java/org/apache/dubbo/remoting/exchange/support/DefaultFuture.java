package org.apache.dubbo.remoting.exchange.support;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.timer.HashedWheelTimer;
import org.apache.dubbo.common.timer.Timeout;
import org.apache.dubbo.common.timer.Timer;
import org.apache.dubbo.common.timer.TimerTask;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.TimeoutException;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.remoting.exchange.ResponseCallback;
import org.apache.dubbo.remoting.exchange.ResponseFuture;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_TIMEOUT;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;

public class DefaultFuture implements ResponseFuture {

    private static final Logger logger = LoggerFactory.getLogger(DefaultFuture.class);

    /**
     * <请求ID, 消息通道>
     */
    private static final Map<Long, Channel> CHANNELS = new ConcurrentHashMap<>();

    /**
     * <请求ID, 未完成状态的RPC请求>
     */
    private static final Map<Long, DefaultFuture> FUTURES = new ConcurrentHashMap<>();

    public static final Timer TIME_OUT_TIMER = new HashedWheelTimer(new NamedThreadFactory("dubbo-future-timeout", true), 30, TimeUnit.MILLISECONDS);

    /**
     * RPC调用的请求ID，构造器中从Request获取
     */
    private final long id;

    /**
     * 消息通道，构造器传入
     */
    private final Channel channel;

    /**
     * RPC请求消息，构造器传入
     */
    private final Request request;

    /**
     * RPC执行超时时间
     */
    private final int timeout;

    private final Lock lock = new ReentrantLock();

    private final Condition done = lock.newCondition();

    /**
     * RPC执行开始时间start
     */
    private final long start = System.currentTimeMillis();

    private volatile long sent;
    /**
     * RPC响应消息
     */
    private volatile Response response;

    /**
     * RPC响应回调器
     */
    private volatile ResponseCallback callback;

    /**
     * 构造器
     */
    private DefaultFuture(Channel channel, Request request, int timeout) {
        this.channel = channel;
        this.request = request;
        // 获取请求id，这个id很重要
        this.id = request.getId();

        this.timeout = timeout > 0 ? timeout : channel.getUrl().getPositiveParameter(TIMEOUT_KEY, DEFAULT_TIMEOUT);
        // <requestId, DefaultFuture>
        FUTURES.put(id, this);
        CHANNELS.put(id, channel);
    }

    private static void timeoutCheck(DefaultFuture future) {
        TimeoutCheckTask task = new TimeoutCheckTask(future);
        // 对已超时的RPC请求，构建相应的超时响应Response并触发received()方法
        TIME_OUT_TIMER.newTimeout(task, future.getTimeout(), TimeUnit.MILLISECONDS);
    }

    public static DefaultFuture newFuture(Channel channel, Request request, int timeout) {
        final DefaultFuture future = new DefaultFuture(channel, request, timeout);

        timeoutCheck(future);
        return future;
    }

    public static DefaultFuture getFuture(long id) {
        return FUTURES.get(id);
    }

    public static boolean hasFuture(Channel channel) {
        return CHANNELS.containsValue(channel);
    }

    public static void sent(Channel channel, Request request) {
        DefaultFuture future = FUTURES.get(request.getId());
        if (future != null) {
            future.doSent();
        }
    }

    public static void closeChannel(Channel channel) {
        for (Map.Entry<Long, Channel> entry : CHANNELS.entrySet()) {
            if (channel.equals(entry.getValue())) {
                DefaultFuture future = getFuture(entry.getKey());
                if (future != null && !future.isDone()) {
                    Response disconnectResponse = new Response(future.getId());
                    disconnectResponse.setStatus(Response.CHANNEL_INACTIVE);
                    disconnectResponse.setErrorMessage("Channel " + channel + " is inactive. Directly return the unFinished request : " + future.getRequest());
                    DefaultFuture.received(channel, disconnectResponse);
                }
            }
        }
    }

    public static void received(Channel channel, Response response) {
        try {
            // 根据调用编号从FUTURES集合中查找指定的DefaultFuture对象
            DefaultFuture future = FUTURES.remove(response.getId());
            if (future != null) {
                // 继续向下调用
                future.doReceived(response);
            } else {
                logger.warn("The timeout response finally returned at " + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date())) + ", response " + response + (channel == null ? "" : ", channel: " + channel.getLocalAddress() + " -> " + channel.getRemoteAddress()));
            }
        } finally {
            CHANNELS.remove(response.getId());
        }
    }

    @Override
    public Object get() throws RemotingException {
        return get(timeout);
    }

    /**
     * 当服务消费者还未接收到调用结果时，用户线程调用get方法会被阻塞住
     * 同步模式下，框架获得DefaultFuture对象后，会立即调用get方法等待
     * 异步模式下则是将该对象封装到FutureAdapter实例中，并将FutureAdapter实例设置到RpcContext中，供用户使用
     * FutureAdapter是一个适配器，将Dubbo中的ResponseFuture与JDK中的Future进行适配
     */
    @Override
    public Object get(int timeout) throws RemotingException {
        if (timeout <= 0) {
            timeout = DEFAULT_TIMEOUT;
        }
        // 检测服务提供方是否成功返回了调用结果
        if (!isDone()) {
            long start = System.currentTimeMillis();
            lock.lock();
            try {
                // 循环检测服务提供方是否成功返回了调用结果
                while (!isDone()) {
                    // 如果调用结果尚未返回，这里等待一段时间
                    done.await(timeout, TimeUnit.MILLISECONDS);
                    // 如果调用结果成功返回，或等待超时，此时跳出while循环，执行后续的逻辑
                    if (isDone() || System.currentTimeMillis() - start > timeout) {
                        break;
                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                lock.unlock();
            }
            // 如果调用结果仍未返回，则抛出超时异常
            if (!isDone()) {
                throw new TimeoutException(sent > 0, channel, getTimeoutMessage(false));
            }
        }
        // 返回调用结果
        return returnFromResponse();
    }

    public void cancel() {
        Response errorResult = new Response(id);
        errorResult.setErrorMessage("request future has been canceled.");
        response = errorResult;
        FUTURES.remove(id);
        CHANNELS.remove(id);
    }

    @Override
    public boolean isDone() {
        // 通过检测response字段为空与否，判断是否收到了调用结果
        return response != null;
    }

    @Override
    public void setCallback(ResponseCallback callback) {
        if (isDone()) {
            invokeCallback(callback);
        } else {
            boolean isdone = false;
            lock.lock();
            try {
                if (!isDone()) {
                    this.callback = callback;
                } else {
                    isdone = true;
                }
            } finally {
                lock.unlock();
            }
            if (isdone) {
                invokeCallback(callback);
            }
        }
    }

    private static class TimeoutCheckTask implements TimerTask {

        private DefaultFuture future;

        TimeoutCheckTask(DefaultFuture future) {
            this.future = future;
        }

        @Override
        public void run(Timeout timeout) {
            if (future == null || future.isDone()) {
                return;
            }
            Response timeoutResponse = new Response(future.getId());
            timeoutResponse.setStatus(future.isSent() ? Response.SERVER_TIMEOUT : Response.CLIENT_TIMEOUT);
            timeoutResponse.setErrorMessage(future.getTimeoutMessage(true));
            DefaultFuture.received(future.getChannel(), timeoutResponse);

        }

    }

    private void invokeCallback(ResponseCallback c) {
        ResponseCallback callbackCopy = c;
        if (callbackCopy == null) {
            throw new NullPointerException("callback cannot be null.");
        }
        Response res = response;
        if (res == null) {
            throw new IllegalStateException("response cannot be null. url:" + channel.getUrl());
        }
        if (res.getStatus() == Response.OK) {
            try {
                callbackCopy.done(res.getResult());
            } catch (Exception e) {
                logger.error("callback invoke error .result:" + res.getResult() + ",url:" + channel.getUrl(), e);
            }
        } else if (res.getStatus() == Response.CLIENT_TIMEOUT || res.getStatus() == Response.SERVER_TIMEOUT) {
            try {
                TimeoutException te = new TimeoutException(res.getStatus() == Response.SERVER_TIMEOUT, channel, res.getErrorMessage());
                callbackCopy.caught(te);
            } catch (Exception e) {
                logger.error("callback invoke error ,url:" + channel.getUrl(), e);
            }
        } else {
            try {
                RuntimeException re = new RuntimeException(res.getErrorMessage());
                callbackCopy.caught(re);
            } catch (Exception e) {
                logger.error("callback invoke error ,url:" + channel.getUrl(), e);
            }
        }
    }

    private Object returnFromResponse() throws RemotingException {
        Response res = response;
        if (res == null) {
            throw new IllegalStateException("response cannot be null");
        }
        // 如果调用结果的状态为Response.OK，则表示调用过程正常，服务提供方成功返回了调用结果
        if (res.getStatus() == Response.OK) {
            return res.getResult();
        }
        // 抛出异常
        if (res.getStatus() == Response.CLIENT_TIMEOUT || res.getStatus() == Response.SERVER_TIMEOUT) {
            throw new TimeoutException(res.getStatus() == Response.SERVER_TIMEOUT, channel, res.getErrorMessage());
        }
        throw new RemotingException(channel, res.getErrorMessage());
    }

    private long getId() {
        return id;
    }

    private Channel getChannel() {
        return channel;
    }

    private boolean isSent() {
        return sent > 0;
    }

    public Request getRequest() {
        return request;
    }

    private int getTimeout() {
        return timeout;
    }

    private long getStartTimestamp() {
        return start;
    }

    private void doSent() {
        sent = System.currentTimeMillis();
    }

    private void doReceived(Response res) {
        lock.lock();
        try {
            // 保存响应对象
            response = res;
            // 唤醒用户线程
            done.signalAll();
        } finally {
            lock.unlock();
        }
        if (callback != null) {
            invokeCallback(callback);
        }
    }

    private String getTimeoutMessage(boolean scan) {
        long nowTimestamp = System.currentTimeMillis();
        return (sent > 0 ? "Waiting server-side response timeout" : "Sending request timeout in client-side") + (scan ? " by scan timer" : "") + ". start time: " + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(start))) + ", end time: " + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date())) + "," + (sent > 0 ? " client elapsed: " + (sent - start) + " ms, server elapsed: " + (nowTimestamp - sent) : " elapsed: " + (nowTimestamp - start)) + " ms, timeout: " + timeout + " ms, request: " + request + ", channel: " + channel.getLocalAddress() + " -> " + channel.getRemoteAddress();
    }

}
