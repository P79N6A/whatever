package org.apache.dubbo.demo.provider;

import org.apache.dubbo.config.*;
import org.apache.dubbo.demo.DemoService;

import java.util.Collections;

public class Application {
    /**
     * In order to make sure multicast registry works, need to specify '-Djava.net.preferIPv4Stack=true' before
     * launch the application
     */
    public static void main(String[] args) throws Exception {
        // 服务实现
        DemoServiceImpl demoService = new DemoServiceImpl();
        // 当前应用配置
        ApplicationConfig applicationConfig = new ApplicationConfig("dubbo-demo-api-provider");
        // 连接注册中心配置
        // RegistryConfig registryConfig = new RegistryConfig("redis://127.0.0.1:6379");
        RegistryConfig registryConfig = new RegistryConfig("zookeeper://127.0.0.1:2181");
        // 服务提供者协议配置
        // ProtocolConfig protocolConfig = new ProtocolConfig();
        // protocolConfig.setName("dubbo");
        // protocolConfig.setPort(12345);
        // protocolConfig.setThreads(200);
        // 注意：ServiceConfig为重对象，内部封装了与注册中心的连接，以及开启服务端口
        // 服务提供者暴露服务配置
        ServiceConfig<DemoService> serviceConfig = new ServiceConfig<DemoService>(); // 此实例很重，封装了与注册中心的连接，请自行缓存，否则可能造成内存和连接泄漏
        serviceConfig.setApplication(applicationConfig);
        serviceConfig.setRegistry(registryConfig); // 多个注册中心可以用setRegistries()
        // serviceConfig.setProtocol(protocol); // 多个协议可以用setProtocols()
        serviceConfig.setInterface(DemoService.class);
        // serviceConfig.setGroup(""); // 服务分组，当一个接口有多种实现时，可以用group区分
        serviceConfig.setRef(demoService);
        serviceConfig.setVersion("1.0.0"); // 当一个接口实现，出现不兼容升级时，可以用版本号过渡，版本号不同的服务相互间不引用
        MethodConfig methodConfig = new MethodConfig();
        methodConfig.setName("sayHello");
        methodConfig.setTimeout(1000);
        serviceConfig.setMethods(Collections.singletonList(methodConfig));
        // 暴露及注册服务
        serviceConfig.export();
        // 防止退出
        System.in.read();
        /*
         * 自定义Filter
         * 1、继承Filter接口
         * 2、META-INF/...
         * 3、<dubbo:provider filter="xxxFilter" /> 或 <dubbo:consumer filter="xxxFilter" />
         *
         * 使用@Activate注解，dubbo就会把注释的Filter作为原生的Filter自动加载，不需要配置provider或consumer的filter
         * 如果不希望自动加载，用<dubbo:consumer filter="-xxxFilter" />
         */
    }

}
