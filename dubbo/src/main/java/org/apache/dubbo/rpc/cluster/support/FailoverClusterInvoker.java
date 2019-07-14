package org.apache.dubbo.rpc.cluster.support;

import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.LoadBalance;
import org.apache.dubbo.rpc.support.RpcUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.apache.dubbo.rpc.cluster.Constants.DEFAULT_RETRIES;
import static org.apache.dubbo.rpc.cluster.Constants.RETRIES_KEY;

public class FailoverClusterInvoker<T> extends AbstractClusterInvoker<T> {

    private static final Logger logger = LoggerFactory.getLogger(FailoverClusterInvoker.class);

    public FailoverClusterInvoker(Directory<T> directory) {
        super(directory);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Result doInvoke(Invocation invocation, final List<Invoker<T>> invokers, LoadBalance loadbalance) throws RpcException {
        List<Invoker<T>> copyInvokers = invokers;
        checkInvokers(copyInvokers, invocation);
        String methodName = RpcUtils.getMethodName(invocation);
        // 获取重试次数
        int len = getUrl().getMethodParameter(methodName, RETRIES_KEY, DEFAULT_RETRIES) + 1;
        if (len <= 0) {
            len = 1;
        }
        RpcException le = null;
        List<Invoker<T>> invoked = new ArrayList<Invoker<T>>(copyInvokers.size());
        Set<String> providers = new HashSet<String>(len);
        // 循环调用，失败重试
        for (int i = 0; i < len; i++) {
            if (i > 0) {
                checkWhetherDestroyed();
                // 在进行重试前重新列举Invoker，好处是，如果某个服务挂了，通过调用list可得到最新可用的Invoker列表
                copyInvokers = list(invocation);
                // 对copyinvokers进行判空检查
                checkInvokers(copyInvokers, invocation);
            }
            // 通过负载均衡选择Invoker
            Invoker<T> invoker = select(loadbalance, invocation, copyInvokers, invoked);
            // 添加到invoker到invoked列表中
            invoked.add(invoker);
            // 设置invoked到RPC上下文中
            RpcContext.getContext().setInvokers((List) invoked);
            try {
                // 调用目标Invoker的invoke方法
                Result result = invoker.invoke(invocation);
                if (le != null && logger.isWarnEnabled()) {
                    logger.warn("Although retry the method " + methodName + " in the service " + getInterface().getName() + " was successful by the provider " + invoker.getUrl().getAddress() + ", but there have been failed providers " + providers + " (" + providers.size() + "/" + copyInvokers.size() + ") from the registry " + directory.getUrl().getAddress() + " on the consumer " + NetUtils.getLocalHost() + " using the dubbo version " + Version.getVersion() + ". Last error is: " + le.getMessage(), le);
                }
                return result;
            } catch (RpcException e) {
                if (e.isBiz()) {
                    throw e;
                }
                le = e;
            } catch (Throwable e) {
                le = new RpcException(e.getMessage(), e);
            } finally {
                providers.add(invoker.getUrl().getAddress());
            }
        }
        // 若重试失败，则抛出异常
        throw new RpcException(le.getCode(), "Failed to invoke the method " + methodName + " in the service " + getInterface().getName() + ". Tried " + len + " times of the providers " + providers + " (" + providers.size() + "/" + copyInvokers.size() + ") from the registry " + directory.getUrl().getAddress() + " on the consumer " + NetUtils.getLocalHost() + " using the dubbo version " + Version.getVersion() + ". Last error is: " + le.getMessage(), le.getCause() != null ? le.getCause() : le);
    }

}
