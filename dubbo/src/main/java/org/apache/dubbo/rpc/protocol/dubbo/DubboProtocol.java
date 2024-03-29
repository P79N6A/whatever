package org.apache.dubbo.rpc.protocol.dubbo;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.constants.RemotingConstants;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.serialize.support.SerializableClassRegistry;
import org.apache.dubbo.common.serialize.support.SerializationOptimizer;
import org.apache.dubbo.common.utils.*;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.Transporter;
import org.apache.dubbo.remoting.exchange.*;
import org.apache.dubbo.remoting.exchange.support.ExchangeHandlerAdapter;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.protocol.AbstractProtocol;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.dubbo.common.constants.CommonConstants.*;
import static org.apache.dubbo.common.constants.ConfigConstants.*;
import static org.apache.dubbo.common.constants.RpcConstants.*;

/**
 * Dubbo协议的Invoker转为Exporter发生在DubboProtocol类的export方法
 * 主要是打开socket侦听服务，并接收客户端发来的各种请求，通讯细节由Dubbo自己实现
 * RMI协议的Invoker转为Exporter发生在RmiProtocol类的export方法
 */
public class DubboProtocol extends AbstractProtocol {

    public static final String NAME = "dubbo";

    public static final int DEFAULT_PORT = 20880;

    private static final String IS_CALLBACK_SERVICE_INVOKE = "_isCallBackServiceInvoke";

    private static DubboProtocol INSTANCE;

    private final Map<String, ExchangeServer> serverMap = new ConcurrentHashMap<>();

    private final Map<String, List<ReferenceCountExchangeClient>> referenceClientMap = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, Object> locks = new ConcurrentHashMap<>();

    private final Set<String> optimizers = new ConcurrentHashSet<>();

    private final ConcurrentMap<String, String> stubServiceMethodsMap = new ConcurrentHashMap<>();

    private ExchangeHandler requestHandler = new ExchangeHandlerAdapter() {

        @Override
        public CompletableFuture<Object> reply(ExchangeChannel channel, Object message) throws RemotingException {
            if (!(message instanceof Invocation)) {
                throw new RemotingException(channel, "Unsupported request: " + (message == null ? null : (message.getClass().getName() + ": " + message)) + ", channel: consumer: " + channel.getRemoteAddress() + " --> provider: " + channel.getLocalAddress());
            }
            Invocation inv = (Invocation) message;
            // 获取Invoker实例
            Invoker<?> invoker = getInvoker(channel, inv);
            // 回调相关，忽略
            if (Boolean.TRUE.toString().equals(inv.getAttachments().get(IS_CALLBACK_SERVICE_INVOKE))) {
                String methodsStr = invoker.getUrl().getParameters().get("methods");
                boolean hasMethod = false;
                if (methodsStr == null || !methodsStr.contains(",")) {
                    hasMethod = inv.getMethodName().equals(methodsStr);
                } else {
                    String[] methods = methodsStr.split(",");
                    for (String method : methods) {
                        if (inv.getMethodName().equals(method)) {
                            hasMethod = true;
                            break;
                        }
                    }
                }
                if (!hasMethod) {
                    logger.warn(new IllegalStateException("The methodName " + inv.getMethodName() + " not found in callback service interface ,invoke will be ignored." + " please update the api interface. url is:" + invoker.getUrl()) + " ,invocation is :" + inv);
                    return null;
                }
            }
            RpcContext rpcContext = RpcContext.getContext();
            rpcContext.setRemoteAddress(channel.getRemoteAddress());
            // 通过Invoker调用具体的服务
            Result result = invoker.invoke(inv);
            if (result instanceof AsyncRpcResult) {
                return ((AsyncRpcResult) result).getResultFuture().thenApply(r -> (Object) r);

            } else {
                return CompletableFuture.completedFuture(result);
            }
        }

        @Override
        public void received(Channel channel, Object message) throws RemotingException {
            if (message instanceof Invocation) {
                reply((ExchangeChannel) channel, message);

            } else {
                super.received(channel, message);
            }
        }

        @Override
        public void connected(Channel channel) throws RemotingException {
            invoke(channel, ON_CONNECT_KEY);
        }

        @Override
        public void disconnected(Channel channel) throws RemotingException {
            if (logger.isDebugEnabled()) {
                // dubbo://192.168.1.108:20880/org.apache.dubbo.demo.DemoService?anyhost=true
                // &application=dubbo-demo-api-provider
                // &bind.ip=192.168.1.108
                // &bind.port=20880
                // &channel.readonly.sent=true
                // &codec=dubbo
                // &deprecated=false
                // &dubbo=2.0.2
                // &dynamic=true
                // &generic=false
                // &heartbeat=60000
                // &interface=org.apache.dubbo.demo.DemoService
                // &methods=sayHello
                // &pid=6780
                // &register=true
                // &release=
                // &revision=1.0.0
                // &sayHello.timeout=1000
                // &side=provider
                // &timestamp=1562557776866
                // &version=1.0.0
                logger.debug("disconnected from " + channel.getRemoteAddress() + ",url:" + channel.getUrl());
            }
            invoke(channel, ON_DISCONNECT_KEY);
        }

        private void invoke(Channel channel, String methodKey) {
            Invocation invocation = createInvocation(channel, channel.getUrl(), methodKey);
            if (invocation != null) {
                try {
                    received(channel, invocation);
                } catch (Throwable t) {
                    logger.warn("Failed to invoke event method " + invocation.getMethodName() + "(), cause: " + t.getMessage(), t);
                }
            }
        }

        private Invocation createInvocation(Channel channel, URL url, String methodKey) {
            String method = url.getParameter(methodKey);
            if (method == null || method.length() == 0) {
                return null;
            }
            RpcInvocation invocation = new RpcInvocation(method, new Class<?>[0], new Object[0]);
            invocation.setAttachment(PATH_KEY, url.getPath());
            invocation.setAttachment(GROUP_KEY, url.getParameter(GROUP_KEY));
            invocation.setAttachment(INTERFACE_KEY, url.getParameter(INTERFACE_KEY));
            invocation.setAttachment(VERSION_KEY, url.getParameter(VERSION_KEY));
            if (url.getParameter(STUB_EVENT_KEY, false)) {
                invocation.setAttachment(STUB_EVENT_KEY, Boolean.TRUE.toString());
            }
            return invocation;
        }
    };

    public DubboProtocol() {
        INSTANCE = this;
    }

    public static DubboProtocol getDubboProtocol() {
        if (INSTANCE == null) {
            ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(DubboProtocol.NAME);
        }
        return INSTANCE;
    }

    public Collection<ExchangeServer> getServers() {
        return Collections.unmodifiableCollection(serverMap.values());
    }

    public Collection<Exporter<?>> getExporters() {
        return Collections.unmodifiableCollection(exporterMap.values());
    }

    Map<String, Exporter<?>> getExporterMap() {
        return exporterMap;
    }

    private boolean isClientSide(Channel channel) {
        InetSocketAddress address = channel.getRemoteAddress();
        URL url = channel.getUrl();
        return url.getPort() == address.getPort() && NetUtils.filterLocalHost(channel.getUrl().getIp()).equals(NetUtils.filterLocalHost(address.getAddress().getHostAddress()));
    }

    Invoker<?> getInvoker(Channel channel, Invocation inv) throws RemotingException {
        // 忽略回调和本地存根相关逻辑
        boolean isCallBackServiceInvoke = false;
        boolean isStubServiceInvoke = false;
        int port = channel.getLocalAddress().getPort();
        String path = inv.getAttachments().get(PATH_KEY);
        isStubServiceInvoke = Boolean.TRUE.toString().equals(inv.getAttachments().get(STUB_EVENT_KEY));
        if (isStubServiceInvoke) {
            port = channel.getRemoteAddress().getPort();
        }
        isCallBackServiceInvoke = isClientSide(channel) && !isStubServiceInvoke;
        if (isCallBackServiceInvoke) {
            path += "." + inv.getAttachments().get(CALLBACK_SERVICE_KEY);
            inv.getAttachments().put(IS_CALLBACK_SERVICE_INVOKE, Boolean.TRUE.toString());
        }
        // 计算service key，格式为groupName/serviceName:serviceVersion:port
        // 比如：dubbo/com.alibaba.dubbo.demo.DemoService:1.0.0:20880
        String serviceKey = serviceKey(port, path, inv.getAttachments().get(VERSION_KEY), inv.getAttachments().get(GROUP_KEY));
        // 从exporterMap查找与serviceKey相对应的DubboExporter对象
        // 服务导出过程中会将 <serviceKey, DubboExporter> 映射关系存储到exporterMap集合中
        DubboExporter<?> exporter = (DubboExporter<?>) exporterMap.get(serviceKey);
        if (exporter == null) {
            throw new RemotingException(channel, "Not found exported service: " + serviceKey + " in " + exporterMap.keySet() + ", may be version or group mismatch " + ", channel: consumer: " + channel.getRemoteAddress() + " --> provider: " + channel.getLocalAddress() + ", message:" + inv);
        }
        // 获取Invoker对象，并返回
        return exporter.getInvoker();
    }

    public Collection<Invoker<?>> getInvokers() {
        return Collections.unmodifiableCollection(invokers);
    }

    @Override
    public int getDefaultPort() {
        return DEFAULT_PORT;
    }

    @Override
    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {

        URL url = invoker.getUrl();
        //  dubbo://192.168.1.100:20880/org.apache.dubbo.demo.DemoService:1.0.0?anyhost=true&application=dubbo-demo-api-provider&bind.ip=192.168.1.100&bind.port=20880&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&interface=org.apache.dubbo.demo.DemoService&methods=sayHello&pid=3580&register=true&release=&revision=1.0.0&sayHello.timeout=1000&side=provider&timestamp=1563110838271&version=1.0.0
        logger.debug("url: " + url);
        // 获取服务标识，理解成服务坐标也行
        // 由服务组名，服务名，服务版本号以及端口组成
        // 比如：org.apache.dubbo.demo.DemoService:1.0.0:1.0.0:20880
        String key = serviceKey(url);
        logger.debug("key: " + key);
        // 创建DubboExporter
        DubboExporter<T> exporter = new DubboExporter<T>(invoker, key, exporterMap);
        // 将 <key, exporter> 键值对放入缓存中
        exporterMap.put(key, exporter);
        // 本地存根相关代码
        Boolean isStubSupportEvent = url.getParameter(STUB_EVENT_KEY, DEFAULT_STUB_EVENT);
        Boolean isCallbackservice = url.getParameter(IS_CALLBACK_SERVICE, false);
        if (isStubSupportEvent && !isCallbackservice) {
            String stubServiceMethods = url.getParameter(STUB_EVENT_METHODS_KEY);
            if (stubServiceMethods == null || stubServiceMethods.length() == 0) {
                if (logger.isWarnEnabled()) {
                    logger.warn(new IllegalStateException("consumer [" + url.getParameter(INTERFACE_KEY) + "], has set stubproxy support event ,but no stub methods founded."));
                }

            } else {
                stubServiceMethodsMap.put(url.getServiceKey(), stubServiceMethods);
            }
        }
        // 启动服务器
        openServer(url);
        // 优化序列化
        optimizeSerialization(url);
        return exporter;
    }

    private void openServer(URL url) {
        // 获取host:port，并将其作为服务器实例的key，用于标识当前的服务器实例
        String key = url.getAddress();
        boolean isServer = url.getParameter(IS_SERVER_KEY, true);
        if (isServer) {
            // 访问缓存
            ExchangeServer server = serverMap.get(key);
            if (server == null) {
                synchronized (this) {
                    server = serverMap.get(key);
                    if (server == null) {
                        // 创建服务器实例
                        serverMap.put(key, createServer(url));
                    }
                }
            } else {
                // 服务器已创建，则根据url中的配置重置服务器，在同一台机器上（单网卡），同一个端口上仅允许启动一个服务器实例
                server.reset(url);
            }
        }
    }

    /**
     * 第一是检测是否存在server参数所代表的Transporter拓展，不存在则抛出异常
     * 第二是创建服务器实例
     * 第三是检测是否支持client参数所表示的Transporter拓展，不存在也是抛出异常
     */
    private ExchangeServer createServer(URL url) {
        url = URLBuilder.from(url).addParameterIfAbsent(RemotingConstants.CHANNEL_READONLYEVENT_SENT_KEY, Boolean.TRUE.toString())
                // heartbeat
                .addParameterIfAbsent(RemotingConstants.HEARTBEAT_KEY, String.valueOf(RemotingConstants.DEFAULT_HEARTBEAT))
                // codec=dubbo
                .addParameter(RemotingConstants.CODEC_KEY, DubboCodec.NAME).build();
        // server=netty
        String str = url.getParameter(RemotingConstants.SERVER_KEY, RemotingConstants.DEFAULT_REMOTING_SERVER);
        // 通过SPI检测是否存在server参数所代表的Transporter拓展，不存在则抛出异常
        if (str != null && str.length() > 0 && !ExtensionLoader.getExtensionLoader(Transporter.class).hasExtension(str)) {
            throw new RpcException("Unsupported server type: " + str + ", url: " + url);
        }
        // dubbo://192.168.1.100:20880/org.apache.dubbo.demo.DemoService:1.0.0?anyhost=true&application=dubbo-demo-api-provider&bind.ip=192.168.1.100&bind.port=20880&channel.readonly.sent=true&codec=dubbo&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&heartbeat=60000&interface=org.apache.dubbo.demo.DemoService&methods=sayHello&pid=3580&register=true&release=&revision=1.0.0&sayHello.timeout=1000&side=provider&timestamp=1563110838271&version=1.0.0
        logger.debug("url: " + url);
        ExchangeServer server;
        try {
            // 创建ExchangeServer
            server = Exchangers.bind(url, requestHandler);
        } catch (RemotingException e) {
            throw new RpcException("Fail to start server(url: " + url + ") " + e.getMessage(), e);
        }
        // 获取client参数，可指定netty
        str = url.getParameter(RemotingConstants.CLIENT_KEY);
        if (str != null && str.length() > 0) {
            // 获取所有的Transporter实现类名称集合，比如supportedTypes = [netty, mina]
            Set<String> supportedTypes = ExtensionLoader.getExtensionLoader(Transporter.class).getSupportedExtensions();
            // 检测当前Dubbo所支持的Transporter实现类名称列表中，是否包含client所表示的Transporter，若不包含，则抛出异常
            if (!supportedTypes.contains(str)) {
                throw new RpcException("Unsupported client type: " + str);
            }
        }
        return server;
    }

    private void optimizeSerialization(URL url) throws RpcException {
        String className = url.getParameter(OPTIMIZER_KEY, "");
        if (StringUtils.isEmpty(className) || optimizers.contains(className)) {
            return;
        }
        logger.info("Optimizing the serialization process for Kryo, FST, etc...");
        try {
            Class clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
            if (!SerializationOptimizer.class.isAssignableFrom(clazz)) {
                throw new RpcException("The serialization optimizer " + className + " isn't an instance of " + SerializationOptimizer.class.getName());
            }
            SerializationOptimizer optimizer = (SerializationOptimizer) clazz.newInstance();
            if (optimizer.getSerializableClasses() == null) {
                return;
            }
            for (Class c : optimizer.getSerializableClasses()) {
                SerializableClassRegistry.registerClass(c);
            }
            optimizers.add(className);

        } catch (ClassNotFoundException e) {
            throw new RpcException("Cannot find the serialization optimizer class: " + className, e);

        } catch (InstantiationException e) {
            throw new RpcException("Cannot instantiate the serialization optimizer class: " + className, e);

        } catch (IllegalAccessException e) {
            throw new RpcException("Cannot instantiate the serialization optimizer class: " + className, e);
        }
    }

    @Override
    public <T> Invoker<T> refer(Class<T> serviceType, URL url) throws RpcException {
        // url: dubbo://192.168.1.100:20880/org.apache.dubbo.demo.DemoService:1.0.0?anyhost=true&application=dubbo-demo-api-consumer&check=false&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&interface=org.apache.dubbo.demo.DemoService&lazy=false&methods=sayHello&pid=1004&register=true&register.ip=192.168.1.100&remote.application=dubbo-demo-api-provider&revision=1.0.0&sayHello.timeout=1000&side=consumer&sticky=false&timestamp=1563110838271&version=1.0.0
        logger.debug("url: " + url);
        optimizeSerialization(url);
        // 创建DubboInvoker
        DubboInvoker<T> invoker = new DubboInvoker<T>(serviceType, url, getClients(url), invokers);
        invokers.add(invoker);
        return invoker;
    }

    /**
     * 获取客户端实例
     * 实例类型为ExchangeClient
     * ExchangeClient不具备通信能力，它需要基于更底层的客户端实例进行通信，比如NettyClient
     * 一个服务引用可能对应多个Invoker，每个Invoker指向了一个Provider，消费者内部会开启多个客户端与提供者的一个服务端进行通信
     * 默认只有一个客户端，称为共享连接，也可以使用多个连接，在 <dubbo:reference connections="3" /> 配置
     */
    private ExchangeClient[] getClients(URL url) {
        // url: dubbo://192.168.1.100:20880/org.apache.dubbo.demo.DemoService:1.0.0?anyhost=true&application=dubbo-demo-api-consumer&check=false&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&interface=org.apache.dubbo.demo.DemoService&lazy=false&methods=sayHello&pid=1004&register=true&register.ip=192.168.1.100&remote.application=dubbo-demo-api-provider&revision=1.0.0&sayHello.timeout=1000&side=consumer&sticky=false&timestamp=1563110838271&version=1.0.0
        logger.debug("url: " + url);
        // 是否共享连接
        boolean useShareConnect = false;
        // 获取连接数，默认为0，表示未配置
        int connections = url.getParameter(CONNECTIONS_KEY, 0);
        List<ReferenceCountExchangeClient> shareClients = null;
        // 如果未配置connections，则共享连接
        if (connections == 0) {
            useShareConnect = true;
            String shareConnectionsStr = url.getParameter(SHARE_CONNECTIONS_KEY, (String) null);
            connections = Integer.parseInt(StringUtils.isBlank(shareConnectionsStr) ? ConfigUtils.getProperty(SHARE_CONNECTIONS_KEY, DEFAULT_SHARE_CONNECTIONS) : shareConnectionsStr);
            shareClients = getSharedClient(url, connections);
        }
        ExchangeClient[] clients = new ExchangeClient[connections];
        for (int i = 0; i < clients.length; i++) {
            if (useShareConnect) {
                // 获取共享客户端
                clients[i] = shareClients.get(i);
            } else {
                // 初始化新的客户端
                clients[i] = initClient(url);
            }
        }
        return clients;
    }

    private List<ReferenceCountExchangeClient> getSharedClient(URL url, int connectNum) {
        String key = url.getAddress();
        // 获取带有“引用计数”功能的ExchangeClient
        List<ReferenceCountExchangeClient> clients = referenceClientMap.get(key);
        if (checkClientCanUse(clients)) {
            batchClientRefIncr(clients);
            return clients;
        }
        locks.putIfAbsent(key, new Object());
        synchronized (locks.get(key)) {
            clients = referenceClientMap.get(key);
            if (checkClientCanUse(clients)) {
                batchClientRefIncr(clients);
                return clients;
            }
            connectNum = Math.max(connectNum, 1);
            if (CollectionUtils.isEmpty(clients)) {
                clients = buildReferenceCountExchangeClientList(url, connectNum);
                referenceClientMap.put(key, clients);

            } else {
                for (int i = 0; i < clients.size(); i++) {
                    ReferenceCountExchangeClient referenceCountExchangeClient = clients.get(i);
                    if (referenceCountExchangeClient == null || referenceCountExchangeClient.isClosed()) {
                        clients.set(i, buildReferenceCountExchangeClient(url));
                        continue;
                    }
                    referenceCountExchangeClient.incrementAndGetCount();
                }
            }
            locks.remove(key);
            return clients;
        }
    }

    private boolean checkClientCanUse(List<ReferenceCountExchangeClient> referenceCountExchangeClients) {
        if (CollectionUtils.isEmpty(referenceCountExchangeClients)) {
            return false;
        }
        for (ReferenceCountExchangeClient referenceCountExchangeClient : referenceCountExchangeClients) {
            if (referenceCountExchangeClient == null || referenceCountExchangeClient.isClosed()) {
                return false;
            }
        }
        return true;
    }

    private void batchClientRefIncr(List<ReferenceCountExchangeClient> referenceCountExchangeClients) {
        if (CollectionUtils.isEmpty(referenceCountExchangeClients)) {
            return;
        }
        for (ReferenceCountExchangeClient referenceCountExchangeClient : referenceCountExchangeClients) {
            if (referenceCountExchangeClient != null) {
                // 增加引用计数
                referenceCountExchangeClient.incrementAndGetCount();
            }
        }
    }

    private List<ReferenceCountExchangeClient> buildReferenceCountExchangeClientList(URL url, int connectNum) {
        List<ReferenceCountExchangeClient> clients = new ArrayList<>();
        for (int i = 0; i < connectNum; i++) {
            clients.add(buildReferenceCountExchangeClient(url));
        }
        return clients;
    }

    private ReferenceCountExchangeClient buildReferenceCountExchangeClient(URL url) {
        // 创建ExchangeClient客户端
        ExchangeClient exchangeClient = initClient(url);
        // 将ExchangeClient实例传给ReferenceCountExchangeClient，装饰模式
        return new ReferenceCountExchangeClient(exchangeClient);
    }

    private ExchangeClient initClient(URL url) {
        // client=netty
        String str = url.getParameter(RemotingConstants.CLIENT_KEY, url.getParameter(RemotingConstants.SERVER_KEY, RemotingConstants.DEFAULT_REMOTING_CLIENT));
        // codec=dubbo
        url = url.addParameter(RemotingConstants.CODEC_KEY, DubboCodec.NAME);
        // heartbeat=
        url = url.addParameterIfAbsent(RemotingConstants.HEARTBEAT_KEY, String.valueOf(RemotingConstants.DEFAULT_HEARTBEAT));
        // 检测客户端类型是否存在，不存在则抛出异常
        if (str != null && str.length() > 0 && !ExtensionLoader.getExtensionLoader(Transporter.class).hasExtension(str)) {
            throw new RpcException("Unsupported client type: " + str + "," + " supported client type is " + StringUtils.join(ExtensionLoader.getExtensionLoader(Transporter.class).getSupportedExtensions(), " "));
        }
        // url: dubbo://192.168.1.100:20880/org.apache.dubbo.demo.DemoService:1.0.0?anyhost=true&application=dubbo-demo-api-consumer&check=false&codec=dubbo&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&heartbeat=60000&interface=org.apache.dubbo.demo.DemoService&lazy=false&methods=sayHello&pid=1004&register=true&register.ip=192.168.1.100&remote.application=dubbo-demo-api-provider&revision=1.0.0&sayHello.timeout=1000&side=consumer&sticky=false&timestamp=1563110838271&version=1.0.0
        logger.debug("url: " + url);
        ExchangeClient client;
        try {
            // lazy配置
            if (url.getParameter(LAZY_CONNECT_KEY, false)) {
                // 懒加载ExchangeClient
                client = new LazyConnectExchangeClient(url, requestHandler);

            } else {
                // 普通ExchangeClient
                client = Exchangers.connect(url, requestHandler);
            }

        } catch (RemotingException e) {
            throw new RpcException("Fail to create remoting client for service(" + url + "): " + e.getMessage(), e);
        }
        return client;
    }

    @Override
    public void destroy() {
        for (String key : new ArrayList<>(serverMap.keySet())) {
            ExchangeServer server = serverMap.remove(key);
            if (server == null) {
                continue;
            }
            try {
                if (logger.isInfoEnabled()) {
                    logger.info("Close dubbo server: " + server.getLocalAddress());
                }
                server.close(ConfigurationUtils.getServerShutdownTimeout());

            } catch (Throwable t) {
                logger.warn(t.getMessage(), t);
            }
        }
        for (String key : new ArrayList<>(referenceClientMap.keySet())) {
            List<ReferenceCountExchangeClient> clients = referenceClientMap.remove(key);
            if (CollectionUtils.isEmpty(clients)) {
                continue;
            }
            for (ReferenceCountExchangeClient client : clients) {
                closeReferenceCountExchangeClient(client);
            }
        }
        stubServiceMethodsMap.clear();
        super.destroy();
    }

    private void closeReferenceCountExchangeClient(ReferenceCountExchangeClient client) {
        if (client == null) {
            return;
        }
        try {
            if (logger.isInfoEnabled()) {
                logger.info("Close dubbo connect: " + client.getLocalAddress() + "-->" + client.getRemoteAddress());
            }
            client.close(ConfigurationUtils.getServerShutdownTimeout());

        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
        }
    }

}
