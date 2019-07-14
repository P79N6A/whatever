package org.apache.dubbo.demo.consumer;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.demo.DemoService;

public class Application {
    /**
     * In order to make sure multicast registry works, need to specify '-Djava.net.preferIPv4Stack=true' before
     * launch the application
     */
    public static void main(String[] args) {
        // 当前应用配置
        ApplicationConfig applicationConfig = new ApplicationConfig("dubbo-demo-api-consumer");
        // 连接注册中心配置
        // RegistryConfig registryConfig = new RegistryConfig("redis://127.0.0.1:6379");
        RegistryConfig registryConfig = new RegistryConfig("zookeeper://127.0.0.1:2181");
        // 注意：ReferenceConfig为重对象，内部封装了与注册中心的连接，以及与服务提供方的连接
        // 引用远程服务
        ReferenceConfig<DemoService> referenceConfig = new ReferenceConfig<>(); // 此实例很重，封装了与注册中心的连接以及与提供者的连接，请自行缓存，否则可能造成内存和连接泄漏
        referenceConfig.setApplication(applicationConfig);
        referenceConfig.setRegistry(registryConfig); // 多个注册中心可以用setRegistries()
        referenceConfig.setInterface(DemoService.class);
        referenceConfig.setVersion("1.0.0");
        // 和本地bean一样使用xxxService
        DemoService demoService = referenceConfig.get(); // 注意：此代理对象内部封装了所有通讯细节，对象较重，请缓存复用

        /*
         * proxy0#sayHello(String)
         *   —> InvokerInvocationHandler#invoke(Object, Method, Object[])
         *     —> MockClusterInvoker#invoke(Invocation)
         *       —> AbstractClusterInvoker#invoke(Invocation)
         *         —> FailoverClusterInvoker#doInvoke(Invocation, List<Invoker<T>>, LoadBalance)
         *           —> Filter#invoke(Invoker, Invocation)  // 包含多个Filter调用
         *             —> ListenerInvokerWrapper#invoke(Invocation)
         *               —> AbstractInvoker#invoke(Invocation)
         *                 —> DubboInvoker#doInvoke(Invocation)
         *                   —> ReferenceCountExchangeClient#request(Object, int)
         *                     —> HeaderExchangeClient#request(Object, int)
         *                       —> HeaderExchangeChannel#request(Object, int)
         *                         —> AbstractPeer#send(Object)
         *                           —> AbstractClient#send(Object, boolean)
         *                             —> NettyChannel#send(Object, boolean)
         *                               —> NioClientSocketChannel#write(Object)
         */
        String message = demoService.sayHello("dubbo");
        System.out.println(message);

    }

}
