package org.apache.dubbo.rpc.filter;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.filter.tps.DefaultTPSLimiter;
import org.apache.dubbo.rpc.filter.tps.TPSLimiter;

import static org.apache.dubbo.common.constants.RpcConstants.TPS_LIMIT_RATE_KEY;

@Activate(group = CommonConstants.PROVIDER, value = TPS_LIMIT_RATE_KEY)
public class TpsLimitFilter implements Filter {

    private final TPSLimiter tpsLimiter = new DefaultTPSLimiter();

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (!tpsLimiter.isAllowable(invoker.getUrl(), invocation)) {
            throw new RpcException("Failed to invoke service " + invoker.getInterface().getName() + "." + invocation.getMethodName() + " because exceed max service tps.");
        }
        return invoker.invoke(invocation);
    }

}
