/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.config;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.bytecode.Wrapper;
import org.apache.dubbo.common.constants.RemotingConstants;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.*;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.support.Parameter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.cluster.Cluster;
import org.apache.dubbo.rpc.cluster.directory.StaticDirectory;
import org.apache.dubbo.rpc.cluster.support.ClusterUtils;
import org.apache.dubbo.rpc.cluster.support.RegistryAwareCluster;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.rpc.protocol.injvm.InjvmProtocol;
import org.apache.dubbo.rpc.service.GenericService;
import org.apache.dubbo.rpc.support.ProtocolUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

import static org.apache.dubbo.common.constants.CommonConstants.*;
import static org.apache.dubbo.common.constants.ConfigConstants.*;
import static org.apache.dubbo.common.constants.MonitorConstants.MONITOR_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.CONSUMER_PROTOCOL;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_PROTOCOL;
import static org.apache.dubbo.common.constants.RpcConstants.LOCAL_PROTOCOL;
import static org.apache.dubbo.common.utils.NetUtils.isInvalidLocalHost;

/**
 * ReferenceConfig
 *
 * @export
 */
public class ReferenceConfig<T> extends AbstractReferenceConfig {

    private static final long serialVersionUID = -5864351140409987595L;

    /**
     * The {@link Protocol} implementation with adaptive functionality,it will be different in different scenarios.
     * A particular {@link Protocol} implementation is determined by the protocol attribute in the {@link URL}.
     * For example:
     *
     * <li>when the url is registry://224.5.6.7:1234/org.apache.dubbo.registry.RegistryService?application=dubbo-sample,
     * then the protocol is <b>RegistryProtocol</b></li>
     *
     * <li>when the url is dubbo://224.5.6.7:1234/org.apache.dubbo.config.api.DemoService?application=dubbo-sample, then
     * the protocol is <b>DubboProtocol</b></li>
     * <p>
     * Actually，when the {@link ExtensionLoader} init the {@link Protocol} instants,it will automatically wraps two
     * layers, and eventually will get a <b>ProtocolFilterWrapper</b> or <b>ProtocolListenerWrapper</b>
     */
    private static final Protocol REF_PROTOCOL = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();

    /**
     * The {@link Cluster}'s implementation with adaptive functionality, and actually it will get a {@link Cluster}'s
     * specific implementation who is wrapped with <b>MockClusterInvoker</b>
     */
    private static final Cluster CLUSTER = ExtensionLoader.getExtensionLoader(Cluster.class).getAdaptiveExtension();

    /**
     * A {@link ProxyFactory} implementation that will generate a reference service's proxy,the JavassistProxyFactory is
     * its default implementation
     */
    private static final ProxyFactory PROXY_FACTORY = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();

    /**
     * The url of the reference service
     */
    private final List<URL> urls = new ArrayList<URL>();

    /**
     * The interface name of the reference service
     */
    private String interfaceName;

    /**
     * The interface class of the reference service
     */
    private Class<?> interfaceClass;

    /**
     * client type
     */
    private String client;

    /**
     * The url for peer-to-peer invocation
     */
    private String url;

    /**
     * The method configs
     */
    private List<MethodConfig> methods;

    /**
     * The consumer config (default)
     */
    private ConsumerConfig consumer;

    /**
     * Only the service provider of the specified protocol is invoked, and other protocols are ignored.
     */
    private String protocol;

    /**
     * The interface proxy reference
     */
    private transient volatile T ref;

    /**
     * The invoker of the reference service
     */
    private transient volatile Invoker<?> invoker;

    /**
     * The flag whether the ReferenceConfig has been initialized
     */
    private transient volatile boolean initialized;

    /**
     * whether this ReferenceConfig has been destroyed
     */
    private transient volatile boolean destroyed;

    @SuppressWarnings("unused")
    private final Object finalizerGuardian = new Object() {
        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            if (!ReferenceConfig.this.destroyed) {
                logger.warn("ReferenceConfig(" + url + ") is not DESTROYED when FINALIZE");

                /* don't destroy for now
                try {
                    ReferenceConfig.this.destroy();
                } catch (Throwable t) {
                        logger.warn("Unexpected err when destroy invoker of ReferenceConfig(" + url + ") in finalize method!", t);
                }
                */
            }
        }
    };

    public ReferenceConfig() {
    }

    public ReferenceConfig(Reference reference) {
        appendAnnotation(Reference.class, reference);
        setMethods(MethodConfig.constructMethodConfig(reference.methods()));
    }

    public URL toUrl() {
        return urls.isEmpty() ? null : urls.iterator().next();
    }

    public List<URL> toUrls() {
        return urls;
    }

    /**
     * This method should be called right after the creation of this class's instance, before any property in other config modules is used.
     * Check each config modules are created properly and override their properties if necessary.
     */
    public void checkAndUpdateSubConfigs() {
        // 接口名非空
        if (StringUtils.isEmpty(interfaceName)) {
            throw new IllegalStateException("<dubbo:reference interface=\"\" /> interface not allow null!");
        }
        completeCompoundConfigs();
        startConfigCenter();
        // get consumer's global configuration
        // 检测consumer变量是否为空，为空则创建
        checkDefault();
        this.refresh();
        if (getGeneric() == null && getConsumer() != null) {
            // 设置generic
            setGeneric(getConsumer().getGeneric());
        }
        // 检测是否为泛化接口调用
        if (ProtocolUtils.isGeneric(getGeneric())) {
            interfaceClass = GenericService.class;
        } else {
            try {
                // 加载类
                interfaceClass = Class.forName(interfaceName, true, Thread.currentThread().getContextClassLoader());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            checkInterfaceAndMethods(interfaceClass, methods);
        }
        resolveFile();
        // 检测Application合法性
        checkApplication();
        checkMetadataReport();
    }

    public synchronized T get() {
        checkAndUpdateSubConfigs();
        if (destroyed) {
            throw new IllegalStateException("The invoker of ReferenceConfig(" + url + ") has already destroyed!");
        }
        if (ref == null) {
            // 处理配置，调用createProxy生成代理类
            init();
        }
        return ref;
    }

    public synchronized void destroy() {
        if (ref == null) {
            return;
        }
        if (destroyed) {
            return;
        }
        destroyed = true;
        try {
            invoker.destroy();
        } catch (Throwable t) {
            logger.warn("Unexpected error occured when destroy invoker of ReferenceConfig(" + url + ").", t);
        }
        invoker = null;
        ref = null;
    }

    private void init() {
        // 避免重复初始化
        if (initialized) {
            return;
        }
        initialized = true;
        // 检测本地存根配置合法性
        checkStubAndLocal(interfaceClass);
        checkMock(interfaceClass);
        Map<String, String> map = new HashMap<String, String>();
        map.put(SIDE_KEY, CONSUMER_SIDE);
        appendRuntimeParameters(map);
        // 非泛化服务
        if (!isGeneric()) {
            // 获取版本
            String revision = Version.getVersion(interfaceClass, version);
            if (revision != null && revision.length() > 0) {
                map.put(REVISION_KEY, revision);
            }
            // 获取接口方法列表，并添加到map中
            String[] methods = Wrapper.getWrapper(interfaceClass).getMethodNames();
            if (methods.length == 0) {
                logger.warn("No method found in service interface " + interfaceClass.getName());
                map.put(METHODS_KEY, ANY_VALUE);
            } else {
                map.put(METHODS_KEY, StringUtils.join(new HashSet<String>(Arrays.asList(methods)), COMMA_SEPARATOR));
            }
        }
        map.put(INTERFACE_KEY, interfaceName);
        // 将ApplicationConfig、ConsumerConfig、ReferenceConfig等对象的字段信息添加到map中
        appendParameters(map, metrics);
        appendParameters(map, application);
        appendParameters(map, module);
        // remove 'default.' prefix for configs from ConsumerConfig
        // appendParameters(map, consumer, Constants.DEFAULT_KEY);
        appendParameters(map, consumer);
        appendParameters(map, this);
        Map<String, Object> attributes = null;
        if (CollectionUtils.isNotEmpty(methods)) {
            attributes = new HashMap<String, Object>();
            // 遍历MethodConfig列表
            for (MethodConfig methodConfig : methods) {
                appendParameters(map, methodConfig, methodConfig.getName());
                String retryKey = methodConfig.getName() + ".retry";
                // 检测map是否包含methodName.retry
                if (map.containsKey(retryKey)) {
                    String retryValue = map.remove(retryKey);
                    if ("false".equals(retryValue)) {
                        // 添加重试次数配置methodName.retries
                        map.put(methodConfig.getName() + ".retries", "0");
                    }
                }
                // 添加MethodConfig中的“属性”字段到attributes，比如onreturn、onthrow、oninvoke
                attributes.put(methodConfig.getName(), convertMethodConfig2AyncInfo(methodConfig));
            }
        }
        // 获取服务消费者ip地址
        String hostToRegistry = ConfigUtils.getSystemProperty(DUBBO_IP_TO_REGISTRY);
        if (StringUtils.isEmpty(hostToRegistry)) {
            hostToRegistry = NetUtils.getLocalHost();
        } else if (isInvalidLocalHost(hostToRegistry)) {
            throw new IllegalArgumentException("Specified invalid registry ip from property:" + DUBBO_IP_TO_REGISTRY + ", value:" + hostToRegistry);
        }
        map.put(REGISTER_IP_KEY, hostToRegistry);
        // 创建代理类
        ref = createProxy(map);
        String serviceKey = URL.buildKey(interfaceName, group, version);
        // 根据服务名，ReferenceConfig，代理类构建ConsumerModel，并存入到ApplicationModel
        ApplicationModel.initConsumerModel(serviceKey, buildConsumerModel(serviceKey, attributes));
    }

    private ConsumerModel buildConsumerModel(String serviceKey, Map<String, Object> attributes) {
        Method[] methods = interfaceClass.getMethods();
        Class serviceInterface = interfaceClass;
        if (interfaceClass == GenericService.class) {
            try {
                serviceInterface = Class.forName(interfaceName);
                methods = serviceInterface.getMethods();
            } catch (ClassNotFoundException e) {
                methods = interfaceClass.getMethods();
            }
        }
        return new ConsumerModel(serviceKey, serviceInterface, ref, methods, attributes);
    }

    @SuppressWarnings({"unchecked", "rawtypes", "deprecation"})
    private T createProxy(Map<String, String> map) {
        // {side=consumer, register.ip=192.168.1.108, release=, methods=sayHello, lazy=false, dubbo=2.0.2, pid=10304, interface=org.apache.dubbo.demo.DemoService, version=1.0.0, revision=1.0.0, application=dubbo-demo-api-consumer, sticky=false, timestamp=1562836868586}
        logger.debug("map: " + map);
        // 本地引用
        if (shouldJvmRefer(map)) {
            // 生成本地引用URL，协议为injvm
            URL url = new URL(LOCAL_PROTOCOL, LOCALHOST_VALUE, 0, interfaceClass.getName()).addParameters(map);
            logger.debug("Using injvm url " + url);
            // 调用refer方法构建InjvmInvoker实例
            invoker = REF_PROTOCOL.refer(interfaceClass, url);
            if (logger.isInfoEnabled()) {
                logger.info("Using injvm service " + interfaceClass.getName());

            }
        }
        // 远程引用
        else {
            logger.debug("url: " + url);
            // url不为空，表明用户可能想指定点对点调用
            if (url != null && url.length() > 0) { // user specified URL, could be peer-to-peer address, or register center's address.
                // 配置多个url时，分号进行分割
                String[] us = SEMICOLON_SPLIT_PATTERN.split(url);
                if (us != null && us.length > 0) {
                    for (String u : us) {
                        URL url = URL.valueOf(u);
                        if (StringUtils.isEmpty(url.getPath())) {
                            // 设置接口全限定名为url路径
                            url = url.setPath(interfaceName);
                        }
                        // 若url协议为registry，表明用户想使用指定的注册中心
                        if (REGISTRY_PROTOCOL.equals(url.getProtocol())) {
                            // 将map转换为查询字符串，并作为refer参数的值添加到url中
                            urls.add(url.addParameterAndEncoded(REFER_KEY, StringUtils.toQueryString(map)));
                        } else {
                            // 合并url，移除服务提供者的一些配置（这些配置来源于用户配置的url属性），比如线程池相关配置
                            // 并保留服务提供者的部分配置，比如版本，group，时间戳等
                            // 最后将合并后的配置设置为url查询字符串中
                            urls.add(ClusterUtils.mergeUrl(url, map));
                        }
                    }
                }
            }
            // 注册中心
            else { // assemble URL from register center's configuration
                // if protocols not injvm checkRegistry
                if (!LOCAL_PROTOCOL.equalsIgnoreCase(getProtocol())) {
                    checkRegistry();
                    // 加载注册中心url
                    List<URL> us = loadRegistries(false);
                    //  [registry://127.0.0.1:2181/org.apache.dubbo.registry.RegistryService?application=dubbo-demo-api-consumer&dubbo=2.0.2&pid=5860&registry=zookeeper&timestamp=1562837617877]
                    logger.debug("us: " + us);
                    if (CollectionUtils.isNotEmpty(us)) {
                        for (URL u : us) {
                            // URL monitorUrl = loadMonitor(u);
                            // if (monitorUrl != null) {
                            //     map.put(MONITOR_KEY, URL.encode(monitorUrl.toFullString()));
                            // }
                            // 添加refer参数到url中，并将url添加到urls中
                            urls.add(u.addParameterAndEncoded(REFER_KEY, StringUtils.toQueryString(map)));
                        }
                    }

                    // 未配置注册中心，抛出异常
                    if (urls.isEmpty()) {
                        throw new IllegalStateException("No such any registry to reference " + interfaceName + " on the consumer " + NetUtils.getLocalHost() + " use dubbo version " + Version.getVersion() + ", please config <dubbo:registry address=\"...\" /> to your spring config.");
                    }
                }
            }
            //  [registry://127.0.0.1:2181/org.apache.dubbo.registry.RegistryService?application=dubbo-demo-api-consumer&dubbo=2.0.2&pid=6140&refer=application%3Ddubbo-demo-api-consumer%26dubbo%3D2.0.2%26interface%3Dorg.apache.dubbo.demo.DemoService%26lazy%3Dfalse%26methods%3DsayHello%26pid%3D6140%26register.ip%3D192.168.1.108%26revision%3D1.0.0%26side%3Dconsumer%26sticky%3Dfalse%26timestamp%3D1562837699631%26version%3D1.0.0&registry=zookeeper&timestamp=1562837701297]
            logger.debug("urls: " + urls);

            // 单个注册中心或服务提供者(服务直连)
            if (urls.size() == 1) {
                // 根据url协议头，调用RegistryProtocol的refer构建Invoker实例
                invoker = REF_PROTOCOL.refer(interfaceClass, urls.get(0));
            }
            // 多个注册中心或多个服务提供者，或者两者混合
            else {
                List<Invoker<?>> invokers = new ArrayList<Invoker<?>>();
                URL registryURL = null;
                // 获取所有的Invoker
                for (URL url : urls) {
                    // 构建Invoker
                    invokers.add(REF_PROTOCOL.refer(interfaceClass, url));
                    if (REGISTRY_PROTOCOL.equals(url.getProtocol())) {
                        registryURL = url; // use last registry url
                    }
                }
                if (registryURL != null) { // registry url is available
                    // use RegistryAwareCluster only when register's CLUSTER is available
                    // 如果注册中心链接不为空，则将使用RegistryAwareCluster
                    URL u = registryURL.addParameter(CLUSTER_KEY, RegistryAwareCluster.NAME);
                    // The invoker wrap relation would be: RegistryAwareClusterInvoker(StaticDirectory) -> FailoverClusterInvoker(RegistryDirectory, will execute route) -> Invoker
                    // 创建StaticDirectory实例，并由Cluster对多个Invoker进行合并
                    invoker = CLUSTER.join(new StaticDirectory(u, invokers));
                }
                // 没有注册中心，是直接引用
                else { // not a registry url, must be direct invoke.
                    invoker = CLUSTER.join(new StaticDirectory(invokers));
                }
            }
        }
        // invoker可用性检查
        if (shouldCheck() && !invoker.isAvailable()) {
            // make it possible for consumer to retry later if provider is temporarily unavailable
            initialized = false;
            throw new IllegalStateException("Failed to check the status of the service " + interfaceName + ". No provider available for the service " + (group == null ? "" : group + "/") + interfaceName + (version == null ? "" : ":" + version) + " from the url " + invoker.getUrl() + " to the consumer " + NetUtils.getLocalHost() + " use dubbo version " + Version.getVersion());
        }
        if (logger.isInfoEnabled()) {
            // redis://127.0.0.1:6379/org.apache.dubbo.registry.RegistryService?anyhost=true
            // &application=dubbo-demo-api-consumer
            // &check=false
            // &deprecated=false
            // &dubbo=2.0.2
            // &dynamic=true
            // &generic=false
            // &interface=org.apache.dubbo.demo.DemoService
            // &lazy=false
            // &methods=sayHello
            // &pid=6220
            // &register=true
            // &register.ip=192.168.1.108
            // &remote.application=dubbo-demo-api-provider
            // &revision=1.0.0
            // &sayHello.timeout=1000
            // &side=consumer
            // &sticky=false
            // &timestamp=1562560440800
            // &version=1.0.0
            logger.info("Refer dubbo service " + interfaceClass.getName() + " from url " + invoker.getUrl());


            // zookeeper://127.0.0.1:2181/org.apache.dubbo.demo.DemoService:1.0.0?anyhost=true
            // &application=dubbo-demo-api-consumer
            // &check=false&deprecated=false
            // &dubbo=2.0.2
            // &dynamic=true
            // &generic=false
            // &interface=org.apache.dubbo.demo.DemoService
            // &lazy=false
            // &methods=sayHello
            // &pid=1652
            // &register=true
            // &register.ip=192.168.1.108
            // &remote.application=dubbo-demo-api-provider
            // &revision=1.0.0
            // &sayHello.timeout=1000
            // &side=consumer
            // &sticky=false
            // &timestamp=1562914201861
            // &version=1.0.0
        }
        // 生成代理类
        // create service proxy
        return (T) PROXY_FACTORY.getProxy(invoker);
    }

    /**
     * Figure out should refer the service in the same JVM from configurations. The default behavior is true
     * 1. if injvm is specified, then use it
     * 2. then if a url is specified, then assume it's a remote call
     * 3. otherwise, check scope parameter
     * 4. if scope is not specified but the target service is provided in the same JVM, then prefer to make the local
     * call, which is the default behavior
     */
    protected boolean shouldJvmRefer(Map<String, String> map) {
        URL tmpUrl = new URL("temp", "localhost", 0, map);
        // 根据url协议、scope及injvm等参数检测是否需要本地引用
        boolean isJvmRefer;
        if (isInjvm() == null) {
            // if a url is specified, don't do local reference
            if (url != null && url.length() > 0) {
                isJvmRefer = false;
            } else {
                // by default, reference local service if there is
                isJvmRefer = InjvmProtocol.getInjvmProtocol().isInjvmRefer(tmpUrl);
            }
        } else {
            isJvmRefer = isInjvm();
        }
        return isJvmRefer;
    }

    protected boolean shouldCheck() {
        Boolean shouldCheck = isCheck();
        if (shouldCheck == null && getConsumer() != null) {
            shouldCheck = getConsumer().isCheck();
        }
        if (shouldCheck == null) {
            // default true
            shouldCheck = true;
        }
        return shouldCheck;
    }

    protected boolean shouldInit() {
        Boolean shouldInit = isInit();
        if (shouldInit == null && getConsumer() != null) {
            shouldInit = getConsumer().isInit();
        }
        if (shouldInit == null) {
            // default is false
            return false;
        }
        return shouldInit;
    }

    private void checkDefault() {
        if (consumer != null) {
            return;
        }
        setConsumer(ConfigManager.getInstance().getDefaultConsumer().orElseGet(() -> {
            ConsumerConfig consumerConfig = new ConsumerConfig();
            consumerConfig.refresh();
            return consumerConfig;
        }));
    }

    private void completeCompoundConfigs() {
        if (consumer != null) {
            if (application == null) {
                setApplication(consumer.getApplication());
            }
            if (module == null) {
                setModule(consumer.getModule());
            }
            if (registries == null) {
                setRegistries(consumer.getRegistries());
            }
            if (monitor == null) {
                setMonitor(consumer.getMonitor());
            }
        }
        if (module != null) {
            if (registries == null) {
                setRegistries(module.getRegistries());
            }
            if (monitor == null) {
                setMonitor(module.getMonitor());
            }
        }
        if (application != null) {
            if (registries == null) {
                setRegistries(application.getRegistries());
            }
            if (monitor == null) {
                setMonitor(application.getMonitor());
            }
        }
    }

    public Class<?> getInterfaceClass() {
        if (interfaceClass != null) {
            return interfaceClass;
        }
        if (isGeneric() || (getConsumer() != null && getConsumer().isGeneric())) {
            return GenericService.class;
        }
        try {
            if (interfaceName != null && interfaceName.length() > 0) {
                this.interfaceClass = Class.forName(interfaceName, true, ClassUtils.getClassLoader());
            }
        } catch (ClassNotFoundException t) {
            throw new IllegalStateException(t.getMessage(), t);
        }
        return interfaceClass;
    }

    /**
     * @param interfaceClass
     * @see #setInterface(Class)
     * @deprecated
     */
    @Deprecated
    public void setInterfaceClass(Class<?> interfaceClass) {
        setInterface(interfaceClass);
    }

    public String getInterface() {
        return interfaceName;
    }

    public void setInterface(String interfaceName) {
        this.interfaceName = interfaceName;
        if (StringUtils.isEmpty(id)) {
            id = interfaceName;
        }
    }

    public void setInterface(Class<?> interfaceClass) {
        if (interfaceClass != null && !interfaceClass.isInterface()) {
            throw new IllegalStateException("The interface class " + interfaceClass + " is not a interface!");
        }
        this.interfaceClass = interfaceClass;
        setInterface(interfaceClass == null ? null : interfaceClass.getName());
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        checkName(RemotingConstants.CLIENT_KEY, client);
        this.client = client;
    }

    @Parameter(excluded = true)
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<MethodConfig> getMethods() {
        return methods;
    }

    @SuppressWarnings("unchecked")
    public void setMethods(List<? extends MethodConfig> methods) {
        this.methods = (List<MethodConfig>) methods;
    }

    public ConsumerConfig getConsumer() {
        return consumer;
    }

    public void setConsumer(ConsumerConfig consumer) {
        ConfigManager.getInstance().addConsumer(consumer);
        this.consumer = consumer;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    // just for test
    Invoker<?> getInvoker() {
        return invoker;
    }

    @Override
    @Parameter(excluded = true)
    public String getPrefix() {
        return DUBBO + ".reference." + interfaceName;
    }

    private void resolveFile() {
        // 从系统变量中获取与接口名对应的属性值
        String resolve = System.getProperty(interfaceName);
        String resolveFile = null;
        if (StringUtils.isEmpty(resolve)) {
            // 从系统属性中获取解析文件路径
            resolveFile = System.getProperty("dubbo.resolve.file");
            if (StringUtils.isEmpty(resolveFile)) {
                // 从指定位置加载配置文件
                File userResolveFile = new File(new File(System.getProperty("user.home")), "dubbo-resolve.properties");
                if (userResolveFile.exists()) {
                    // 获取文件绝对路径
                    resolveFile = userResolveFile.getAbsolutePath();
                }
            }
            if (resolveFile != null && resolveFile.length() > 0) {
                Properties properties = new Properties();
                try (FileInputStream fis = new FileInputStream(new File(resolveFile))) {
                    // 从文件中加载配置
                    properties.load(fis);
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to load " + resolveFile + ", cause: " + e.getMessage(), e);
                }
                // 获取与接口名对应的配置
                resolve = properties.getProperty(interfaceName);
            }
        }
        if (resolve != null && resolve.length() > 0) {
            // 将resolve赋值给url
            url = resolve;
            if (logger.isWarnEnabled()) {
                if (resolveFile != null) {
                    logger.warn("Using default dubbo resolve file " + resolveFile + " replace " + interfaceName + "" + resolve + " to p2p invoke remote service.");
                } else {
                    logger.warn("Using -D" + interfaceName + "=" + resolve + " to p2p invoke remote service.");
                }
            }
        }
    }

}
