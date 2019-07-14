package org.apache.dubbo.rpc;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.SPI;

import static org.apache.dubbo.common.constants.RpcConstants.PROXY_KEY;

@SPI("javassist")
public interface ProxyFactory {


    /**
     * 针对Client端，创建接口（如DemoService）的代理对象
     */
    @Adaptive({PROXY_KEY})
    <T> T getProxy(Invoker<T> invoker) throws RpcException;

    @Adaptive({PROXY_KEY})
    <T> T getProxy(Invoker<T> invoker, boolean generic) throws RpcException;


    /**
     * 针对Server端，将服务对象（如DemoServiceImpl）包装成一个Invoker对象
     */
    @Adaptive({PROXY_KEY})
    <T> Invoker<T> getInvoker(T proxy, Class<T> type, URL url) throws RpcException;

}