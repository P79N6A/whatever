package org.apache.dubbo.rpc.filter;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.*;

@Activate(group = CommonConstants.CONSUMER, order = -10000)
public class ConsumerContextFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        RpcContext.getContext().setInvoker(invoker).setInvocation(invocation).setLocalAddress(NetUtils.getLocalHost(), 0).setRemoteAddress(invoker.getUrl().getHost(), invoker.getUrl().getPort());
        if (invocation instanceof RpcInvocation) {
            ((RpcInvocation) invocation).setInvoker(invoker);
        }
        try {
            RpcContext.removeServerContext();
            return invoker.invoke(invocation);
        } finally {
            RpcContext.getContext().clearAttachments();
        }
    }

    @Override
    public Result onResponse(Result result, Invoker<?> invoker, Invocation invocation) {
        RpcContext.getServerContext().setAttachments(result.getAttachments());
        return result;
    }

}
