package org.apache.dubbo.rpc.protocol.dubbo;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.constants.RemotingConstants;
import org.apache.dubbo.common.utils.AtomicPositiveInteger;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.TimeoutException;
import org.apache.dubbo.remoting.exchange.ExchangeClient;
import org.apache.dubbo.remoting.exchange.ResponseFuture;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.protocol.AbstractInvoker;
import org.apache.dubbo.rpc.support.RpcUtils;

import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import static org.apache.dubbo.common.constants.CommonConstants.*;
import static org.apache.dubbo.common.constants.RpcConstants.TOKEN_KEY;

public class DubboInvoker<T> extends AbstractInvoker<T> {

    private final ExchangeClient[] clients;

    private final AtomicPositiveInteger index = new AtomicPositiveInteger();

    private final String version;

    private final ReentrantLock destroyLock = new ReentrantLock();

    private final Set<Invoker<?>> invokers;

    public DubboInvoker(Class<T> serviceType, URL url, ExchangeClient[] clients) {
        this(serviceType, url, clients, null);
    }

    public DubboInvoker(Class<T> serviceType, URL url, ExchangeClient[] clients, Set<Invoker<?>> invokers) {
        super(serviceType, url, new String[]{INTERFACE_KEY, GROUP_KEY, TOKEN_KEY, TIMEOUT_KEY});
        this.clients = clients;
        this.version = url.getParameter(VERSION_KEY, "0.0.0");
        this.invokers = invokers;
    }

    /**
     * Dubbo实现同步和异步调用关键在于由谁调用ResponseFuture的get方法
     * 同步调用模式下，由框架自身调用ResponseFuture的get方法
     * 异步调用模式下，则由用户调用该方法
     */
    @Override
    protected Result doInvoke(final Invocation invocation) throws Throwable {
        RpcInvocation inv = (RpcInvocation) invocation;
        final String methodName = RpcUtils.getMethodName(invocation);
        // 设置path和version到attachment中
        inv.setAttachment(PATH_KEY, getUrl().getPath());
        inv.setAttachment(VERSION_KEY, version);
        ExchangeClient currentClient;
        if (clients.length == 1) {
            // 从clients数组中获取ExchangeClient
            currentClient = clients[0];
        } else {
            currentClient = clients[index.getAndIncrement() % clients.length];
        }
        try {
            // 获取异步配置
            boolean isAsync = RpcUtils.isAsync(getUrl(), invocation);
            boolean isAsyncFuture = RpcUtils.isReturnTypeFuture(inv);
            boolean isOneway = RpcUtils.isOneway(getUrl(), invocation);
            int timeout = getUrl().getMethodParameter(methodName, TIMEOUT_KEY, DEFAULT_TIMEOUT);
            // 1 异步无返回值
            if (isOneway) {
                boolean isSent = getUrl().getMethodParameter(methodName, RemotingConstants.SENT_KEY, false);
                // 发送请求
                currentClient.send(inv, isSent);
                // 设置上下文中的future字段为null
                RpcContext.getContext().setFuture(null);
                // 返回一个空的RpcResult
                return new RpcResult();
            }
            // 2 异步有返回值
            else if (isAsync) {
                // 发送请求，返回ResponseFuture
                ResponseFuture future = currentClient.request(inv, timeout);

                FutureAdapter<Object> futureAdapter = new FutureAdapter<>(future);
                // 设置future到上下文中
                RpcContext.getContext().setFuture(futureAdapter);
                Result result;
                if (isAsyncFuture) {
                    result = new AsyncRpcResult(futureAdapter, futureAdapter.getResultFuture(), false);
                } else {
                    result = new SimpleAsyncRpcResult(futureAdapter, futureAdapter.getResultFuture(), false);
                }
                // 暂时返回一个空结果
                return result;
            }
            // 3 同步调用
            else {
                RpcContext.getContext().setFuture(null);
                // 发送请求，返回ResponseFuture，调用get方法等待，异步转同步
                return (Result) currentClient.request(inv, timeout).get();
            }
        } catch (TimeoutException e) {
            throw new RpcException(RpcException.TIMEOUT_EXCEPTION, "Invoke remote method timeout. method: " + invocation.getMethodName() + ", provider: " + getUrl() + ", cause: " + e.getMessage(), e);
        } catch (RemotingException e) {
            throw new RpcException(RpcException.NETWORK_EXCEPTION, "Failed to invoke remote method: " + invocation.getMethodName() + ", provider: " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        if (!super.isAvailable()) {
            return false;
        }
        for (ExchangeClient client : clients) {
            if (client.isConnected() && !client.hasAttribute(RemotingConstants.CHANNEL_ATTRIBUTE_READONLY_KEY)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void destroy() {
        if (super.isDestroyed()) {
            return;
        } else {
            destroyLock.lock();
            try {
                if (super.isDestroyed()) {
                    return;
                }
                super.destroy();
                if (invokers != null) {
                    invokers.remove(this);
                }
                for (ExchangeClient client : clients) {
                    try {
                        client.close(ConfigurationUtils.getServerShutdownTimeout());
                    } catch (Throwable t) {
                        logger.warn(t.getMessage(), t);
                    }
                }

            } finally {
                destroyLock.unlock();
            }
        }
    }

}
