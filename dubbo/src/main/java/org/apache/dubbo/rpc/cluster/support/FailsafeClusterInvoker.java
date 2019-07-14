package org.apache.dubbo.rpc.cluster.support;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.LoadBalance;

import java.util.List;

/**
 * 失败安全是指，当调用过程中出现异常时，仅会打印异常，而不会抛出异常
 * 适用于写入审计日志等操作
 */
public class FailsafeClusterInvoker<T> extends AbstractClusterInvoker<T> {
    private static final Logger logger = LoggerFactory.getLogger(FailsafeClusterInvoker.class);

    public FailsafeClusterInvoker(Directory<T> directory) {
        super(directory);
    }

    @Override
    public Result doInvoke(Invocation invocation, List<Invoker<T>> invokers, LoadBalance loadbalance) throws RpcException {
        try {
            checkInvokers(invokers, invocation);
            // 选择Invoker
            Invoker<T> invoker = select(loadbalance, invocation, invokers, null);
            // 进行远程调用
            return invoker.invoke(invocation);
        } catch (Throwable e) {
            // 打印错误日志，但不抛出
            logger.error("Failsafe ignore exception: " + e.getMessage(), e);
            // 返回空结果忽略错误
            return new RpcResult();
        }
    }

}
