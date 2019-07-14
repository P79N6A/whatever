package org.apache.dubbo.rpc.proxy.javassist;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.bytecode.Proxy;
import org.apache.dubbo.common.bytecode.Wrapper;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.proxy.AbstractProxyFactory;
import org.apache.dubbo.rpc.proxy.AbstractProxyInvoker;
import org.apache.dubbo.rpc.proxy.InvokerInvocationHandler;

public class JavassistProxyFactory extends AbstractProxyFactory {

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProxy(Invoker<T> invoker, Class<?>[] interfaces) {
        // 生成抽象类Proxy子类，并调用Proxy子类的newInstance方法创建Proxy实例
        return (T) Proxy.getProxy(interfaces).newInstance(new InvokerInvocationHandler(invoker));
    }

    /**
     * Invoker是实体域，是Dubbo的核心模型，代表一个可执行体
     * 它可能是一个本地的实现，也可能是一个远程的实现，也可能一个集群实现
     * Invoker是由ProxyFactory创建，默认的ProxyFactory实现类是JavassistProxyFactory
     */
    @Override
    public <T> Invoker<T> getInvoker(T proxy, Class<T> type, URL url) {
        // 为目标类创建Wrapper
        final Wrapper wrapper = Wrapper.getWrapper(proxy.getClass().getName().indexOf('$') < 0 ? proxy.getClass() : type);
        // 创建匿名Invoker类对象，并实现doInvoke方法
        return new AbstractProxyInvoker<T>(proxy, type, url) {
            @Override
            protected Object doInvoke(T proxy, String methodName, Class<?>[] parameterTypes, Object[] arguments) throws Throwable {
                // 调用Wrapper的invokeMethod方法，invokeMethod最终会调用目标方法
                return wrapper.invokeMethod(proxy, methodName, parameterTypes, arguments);
            }
        };
        /*
         * 以DemoServiceImpl为例，Javassist为其生成的代理类如下
         *
         * public class Wrapper0 extends Wrapper implements ClassGenerator.DC {
         *     public static String[] pns;
         *     public static Map pts;
         *     public static String[] mns;
         *     public static String[] dmns;
         *     public static Class[] mts0;
         *
         *     // 省略其他方法，invokeMethod方法内部用对象直接去调用方法，不用反射
         *     public Object invokeMethod(Object object, String string, Class[] arrclass, Object[] arrobject) throws InvocationTargetException {
         *         DemoService demoService;
         *         try {
         *             // 类型转换
         *             demoService = (DemoService) object;
         *         } catch (Throwable throwable) {
         *             throw new IllegalArgumentException(throwable);
         *         }
         *         try {
         *             // 根据方法名调用指定的方法
         *             if ("sayHello".equals(string) && arrclass.length == 1) {
         *                 return demoService.sayHello((String) arrobject[0]);
         *             }
         *         } catch (Throwable throwable) {
         *             throw new InvocationTargetException(throwable);
         *         }
         *         throw new NoSuchMethodException(new StringBuffer().append("Not found method \"").append(string).append("\" in class com.alibaba.dubbo.demo.DemoService.").toString());
         *     }
         * }
         */
    }

}
