package org.apache.dubbo.rpc.filter;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

import static org.apache.dubbo.common.constants.RpcConstants.$ECHO;

@Activate(group = CommonConstants.PROVIDER, order = -110000)
public class EchoFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation inv) throws RpcException {
        if (inv.getMethodName().equals($ECHO) && inv.getArguments() != null && inv.getArguments().length == 1) {
            return new RpcResult(inv.getArguments()[0]);
        }
        return invoker.invoke(inv);
    }

}
