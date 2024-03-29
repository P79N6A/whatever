package org.apache.dubbo.rpc.filter;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.*;
import static org.apache.dubbo.common.constants.RpcConstants.*;

@Activate(group = PROVIDER, order = -10000)
public class ContextFilter implements Filter {
    private static final String TAG_KEY = "dubbo.tag";

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        Map<String, String> attachments = invocation.getAttachments();
        if (attachments != null) {
            attachments = new HashMap<>(attachments);
            attachments.remove(PATH_KEY);
            attachments.remove(INTERFACE_KEY);
            attachments.remove(GROUP_KEY);
            attachments.remove(VERSION_KEY);
            attachments.remove(DUBBO_VERSION_KEY);
            attachments.remove(TOKEN_KEY);
            attachments.remove(TIMEOUT_KEY);
            attachments.remove(ASYNC_KEY);
            attachments.remove(TAG_KEY);
            attachments.remove(FORCE_USE_TAG);
        }
        RpcContext.getContext().setInvoker(invoker).setInvocation(invocation).setLocalAddress(invoker.getUrl().getHost(), invoker.getUrl().getPort()).setRemoteApplicationName(invoker.getUrl().getParameter(REMOTE_APPLICATION_KEY));
        if (attachments != null) {
            if (RpcContext.getContext().getAttachments() != null) {
                RpcContext.getContext().getAttachments().putAll(attachments);
            } else {
                RpcContext.getContext().setAttachments(attachments);
            }
        }
        if (invocation instanceof RpcInvocation) {
            ((RpcInvocation) invocation).setInvoker(invoker);
        }
        try {
            return invoker.invoke(invocation);
        } finally {
            RpcContext.removeContext();
            RpcContext.removeServerContext();
        }
    }

    @Override
    public Result onResponse(Result result, Invoker<?> invoker, Invocation invocation) {
        result.addAttachments(RpcContext.getServerContext().getAttachments());
        return result;
    }

}
