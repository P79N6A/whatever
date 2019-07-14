package org.apache.dubbo.rpc.filter;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.apache.dubbo.common.constants.RpcConstants.ACTIVES_KEY;

@Activate(group = CONSUMER, value = ACTIVES_KEY)
public class ActiveLimitFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        URL url = invoker.getUrl();
        String methodName = invocation.getMethodName();
        int max = invoker.getUrl().getMethodParameter(methodName, ACTIVES_KEY, 0);
        RpcStatus count = RpcStatus.getStatus(invoker.getUrl(), invocation.getMethodName());
        if (!RpcStatus.beginCount(url, methodName, max)) {
            long timeout = invoker.getUrl().getMethodParameter(invocation.getMethodName(), TIMEOUT_KEY, 0);
            long start = System.currentTimeMillis();
            long remain = timeout;
            synchronized (count) {
                while (!RpcStatus.beginCount(url, methodName, max)) {
                    try {
                        count.wait(remain);
                    } catch (InterruptedException e) {
                    }
                    long elapsed = System.currentTimeMillis() - start;
                    remain = timeout - elapsed;
                    if (remain <= 0) {
                        throw new RpcException("Waiting concurrent invoke timeout in client-side for service:  " + invoker.getInterface().getName() + ", method: " + invocation.getMethodName() + ", elapsed: " + elapsed + ", timeout: " + timeout + ". concurrent invokes: " + count.getActive() + ". max concurrent invoke limit: " + max);
                    }
                }
            }
        }
        boolean isSuccess = true;
        long begin = System.currentTimeMillis();
        try {
            return invoker.invoke(invocation);
        } catch (RuntimeException t) {
            isSuccess = false;
            throw t;
        } finally {
            RpcStatus.endCount(url, methodName, System.currentTimeMillis() - begin, isSuccess);
            if (max > 0) {
                synchronized (count) {
                    count.notifyAll();
                }
            }
        }
    }

}
