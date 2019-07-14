package org.apache.dubbo.rpc.proxy;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.support.RpcUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import static org.apache.dubbo.common.constants.RpcConstants.ASYNC_KEY;
import static org.apache.dubbo.common.constants.RpcConstants.FUTURE_RETURNTYPE_KEY;

public class InvokerInvocationHandler implements InvocationHandler {
    private static final Logger logger = LoggerFactory.getLogger(InvokerInvocationHandler.class);

    private final Invoker<?> invoker;

    public InvokerInvocationHandler(Invoker<?> handler) {
        this.invoker = handler;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();
        // 拦截定义在Object类中的方法（未被子类重写），如wait/notify
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(invoker, args);
        }
        // 如果toString、hashCode和equals等方法被子类重写了，也直接调用
        if ("toString".equals(methodName) && parameterTypes.length == 0) {
            return invoker.toString();
        }
        if ("hashCode".equals(methodName) && parameterTypes.length == 0) {
            return invoker.hashCode();
        }
        if ("equals".equals(methodName) && parameterTypes.length == 1) {
            return invoker.equals(args[0]);
        }
        // 将method和args封装到RpcInvocation中，并执行后续的调用
        return invoker.invoke(createInvocation(method, args)).recreate();
    }

    private RpcInvocation createInvocation(Method method, Object[] args) {
        RpcInvocation invocation = new RpcInvocation(method, args);
        if (RpcUtils.hasFutureReturnType(method)) {
            invocation.setAttachment(FUTURE_RETURNTYPE_KEY, "true");
            invocation.setAttachment(ASYNC_KEY, "true");
        }
        return invocation;
    }

}
