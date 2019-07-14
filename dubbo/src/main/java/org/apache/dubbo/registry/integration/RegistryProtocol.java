package org.apache.dubbo.registry.integration;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.configcenter.DynamicConfiguration;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.RegistryFactory;
import org.apache.dubbo.registry.RegistryService;
import org.apache.dubbo.registry.support.ProviderConsumerRegTable;
import org.apache.dubbo.registry.support.ProviderInvokerWrapper;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.cluster.Cluster;
import org.apache.dubbo.rpc.cluster.Configurator;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.protocol.InvokerWrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.apache.dubbo.common.constants.CommonConstants.*;
import static org.apache.dubbo.common.constants.ConfigConstants.*;
import static org.apache.dubbo.common.constants.FilterConstants.VALIDATION_KEY;
import static org.apache.dubbo.common.constants.MonitorConstants.MONITOR_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.*;
import static org.apache.dubbo.common.constants.RemotingConstants.*;
import static org.apache.dubbo.common.constants.RpcConstants.*;
import static org.apache.dubbo.common.utils.UrlUtils.classifyUrls;
import static org.apache.dubbo.rpc.cluster.Constants.*;

public class RegistryProtocol implements Protocol {

    public static final String[] DEFAULT_REGISTER_PROVIDER_KEYS = {APPLICATION_KEY, CODEC_KEY, EXCHANGER_KEY, SERIALIZATION_KEY, CLUSTER_KEY, CONNECTIONS_KEY, DEPRECATED_KEY, GROUP_KEY, LOADBALANCE_KEY, MOCK_KEY, PATH_KEY, TIMEOUT_KEY, TOKEN_KEY, VERSION_KEY, WARMUP_KEY, WEIGHT_KEY, TIMESTAMP_KEY, DUBBO_VERSION_KEY, RELEASE_KEY};

    public static final String[] DEFAULT_REGISTER_CONSUMER_KEYS = {APPLICATION_KEY, VERSION_KEY, GROUP_KEY, DUBBO_VERSION_KEY, RELEASE_KEY};

    private final static Logger logger = LoggerFactory.getLogger(RegistryProtocol.class);

    private static RegistryProtocol INSTANCE;

    private final Map<URL, NotifyListener> overrideListeners = new ConcurrentHashMap<>();

    private final Map<String, ServiceConfigurationListener> serviceConfigurationListeners = new ConcurrentHashMap<>();

    private final ProviderConfigurationListener providerConfigurationListener = new ProviderConfigurationListener();

    private final ConcurrentMap<String, ExporterChangeableWrapper<?>> bounds = new ConcurrentHashMap<>();

    private Cluster cluster;

    private Protocol protocol;

    private RegistryFactory registryFactory;

    private ProxyFactory proxyFactory;

    public RegistryProtocol() {
        INSTANCE = this;
    }

    public static RegistryProtocol getRegistryProtocol() {
        if (INSTANCE == null) {
            ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(REGISTRY_PROTOCOL);
        }
        return INSTANCE;
    }

    private static String[] getFilteredKeys(URL url) {
        Map<String, String> params = url.getParameters();
        if (CollectionUtils.isNotEmptyMap(params)) {
            return params.keySet().stream().filter(k -> k.startsWith(HIDE_KEY_PREFIX)).toArray(String[]::new);
        } else {
            return new String[0];
        }
    }

    /**
     * 注入
     */
    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }
    /**
     * 注入
     */
    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    /**
     * 注入
     */
    public void setRegistryFactory(RegistryFactory registryFactory) {
        this.registryFactory = registryFactory;
    }
    /**
     * 注入
     */
    public void setProxyFactory(ProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    @Override
    public int getDefaultPort() {
        return 9090;
    }

    public Map<URL, NotifyListener> getOverrideListeners() {
        return overrideListeners;
    }

    public void register(URL registryUrl, URL registeredProviderUrl) {

        logger.debug("registryUrl: " + registryUrl);
        logger.debug("registeredProviderUrl: " + registeredProviderUrl);

        // 获取Registry
        Registry registry = registryFactory.getRegistry(registryUrl);
        // 注册服务
        registry.register(registeredProviderUrl);
    }

    public void unregister(URL registryUrl, URL registeredProviderUrl) {
        Registry registry = registryFactory.getRegistry(registryUrl);
        registry.unregister(registeredProviderUrl);
    }

    /**
     * 导出服务到远程
     * <p>
     * 调用doLocalExport导出服务
     * 向注册中心注册服务
     * 向注册中心进行订阅override数据
     * 创建并返回DestroyableExporter
     */
    @Override
    public <T> Exporter<T> export(final Invoker<T> originInvoker) throws RpcException {
        // 获取注册中心URL，以zookeeper为例，得到的URL如下：
        // zookeeper://127.0.0.1:2181/org.apache.dubbo.registry.RegistryService?application=dubbo-demo-api-provider
        // &dubbo=2.0.2
        // &export=dubbo%3A%2F%2F127.0.0.1%3A20880%2Forg.apache.dubbo.demo.DemoService%3Fanyhost%3Dtrue%26application%3Ddubbo-demo-api-provider
        URL registryUrl = getRegistryUrl(originInvoker);
        URL providerUrl = getProviderUrl(originInvoker);

        logger.debug("registryUrl: " + registryUrl);
        logger.debug("providerUrl: " + providerUrl);

        // 获取订阅URL，如：
        // provider://127.0.0.1:20880/org.apache.dubbo.demo.DemoService?
        // &check=false
        // &anyhost=true
        // &application=dubbo-demo-api-provider
        // &dubbo=2.0.2
        // &generic=false
        // &interface=org.apache.dubbo.demo.DemoService
        // &methods=sayHello
        final URL overrideSubscribeUrl = getSubscribedOverrideUrl(providerUrl);
        // 创建监听器
        final OverrideListener overrideSubscribeListener = new OverrideListener(overrideSubscribeUrl, originInvoker);
        overrideListeners.put(overrideSubscribeUrl, overrideSubscribeListener);
        providerUrl = overrideUrlWithConfig(providerUrl, overrideSubscribeListener);
        logger.debug("providerUrl: " + providerUrl);
        // 导出服务
        final ExporterChangeableWrapper<T> exporter = doLocalExport(originInvoker, providerUrl);
        // 根据URL加载Registry实现类，如ZookeeperRegistry
        final Registry registry = getRegistry(originInvoker);
        // 获取已注册的服务提供者URL，如：
        // dubbo://127.0.0.1:20880/org.apache.dubbo.demo.DemoService?anyhost=true
        // &application=dubbo-demo-api-provider
        // &dubbo=2.0.2
        // &generic=false
        // &interface=org.apache.dubbo.demo.DemoService
        // &methods=sayHello
        final URL registeredProviderUrl = getRegisteredProviderUrl(providerUrl, registryUrl);

        logger.debug("registeredProviderUrl: " + registeredProviderUrl);
        // 向服务提供者与消费者注册表中注册服务提供者
        ProviderInvokerWrapper<T> providerInvokerWrapper = ProviderConsumerRegTable.registerProvider(originInvoker, registryUrl, registeredProviderUrl);
        // 获取register参数
        boolean register = registeredProviderUrl.getParameter("register", true);
        // 根据register的值决定是否注册服务
        if (register) {
            // 向注册中心注册服务
            register(registryUrl, registeredProviderUrl);
            providerInvokerWrapper.setReg(true);
        }
        // 向注册中心进行订阅override数据
        registry.subscribe(overrideSubscribeUrl, overrideSubscribeListener);
        exporter.setRegisterUrl(registeredProviderUrl);
        exporter.setSubscribeUrl(overrideSubscribeUrl);
        // 创建并返回DestroyableExporter
        return new DestroyableExporter<>(exporter);
    }

    private URL overrideUrlWithConfig(URL providerUrl, OverrideListener listener) {
        providerUrl = providerConfigurationListener.overrideUrl(providerUrl);
        ServiceConfigurationListener serviceConfigurationListener = new ServiceConfigurationListener(providerUrl, listener);
        serviceConfigurationListeners.put(providerUrl.getServiceKey(), serviceConfigurationListener);
        return serviceConfigurationListener.overrideUrl(providerUrl);
    }

    @SuppressWarnings("unchecked")
    private <T> ExporterChangeableWrapper<T> doLocalExport(final Invoker<T> originInvoker, URL providerUrl) {
        String key = getCacheKey(originInvoker);
        return (ExporterChangeableWrapper<T>) bounds.computeIfAbsent(key, s -> {
            // 创建Invoker为委托类对象
            Invoker<?> invokerDelegate = new InvokerDelegate<>(originInvoker, providerUrl);
            // 调用protocol的export方法导出服务
            // 假设运行时协议为dubbo，此处的protocol变量会在运行时加载DubboProtocol，并调用DubboProtocol的export方法
            return new ExporterChangeableWrapper<>((Exporter<T>) protocol.export(invokerDelegate), originInvoker);
        });
    }

    public <T> void reExport(final Invoker<T> originInvoker, URL newInvokerUrl) {
        ExporterChangeableWrapper exporter = doChangeLocalExport(originInvoker, newInvokerUrl);
        URL registryUrl = getRegistryUrl(originInvoker);
        final URL registeredProviderUrl = getRegisteredProviderUrl(newInvokerUrl, registryUrl);
        ProviderInvokerWrapper<T> providerInvokerWrapper = ProviderConsumerRegTable.getProviderWrapper(registeredProviderUrl, originInvoker);
        ProviderInvokerWrapper<T> newProviderInvokerWrapper = ProviderConsumerRegTable.registerProvider(originInvoker, registryUrl, registeredProviderUrl);
        if (providerInvokerWrapper.isReg() && !registeredProviderUrl.equals(providerInvokerWrapper.getProviderUrl())) {
            unregister(registryUrl, providerInvokerWrapper.getProviderUrl());
            register(registryUrl, registeredProviderUrl);
            newProviderInvokerWrapper.setReg(true);
        }
        exporter.setRegisterUrl(registeredProviderUrl);
    }

    @SuppressWarnings("unchecked")
    private <T> ExporterChangeableWrapper doChangeLocalExport(final Invoker<T> originInvoker, URL newInvokerUrl) {
        String key = getCacheKey(originInvoker);
        final ExporterChangeableWrapper<T> exporter = (ExporterChangeableWrapper<T>) bounds.get(key);
        if (exporter == null) {
            logger.warn(new IllegalStateException("error state, exporter should not be null"));
        } else {
            final Invoker<T> invokerDelegate = new InvokerDelegate<T>(originInvoker, newInvokerUrl);
            exporter.setExporter(protocol.export(invokerDelegate));
        }
        return exporter;
    }

    private Registry getRegistry(final Invoker<?> originInvoker) {
        URL registryUrl = getRegistryUrl(originInvoker);
        return registryFactory.getRegistry(registryUrl);
    }

    private URL getRegistryUrl(Invoker<?> originInvoker) {
        URL registryUrl = originInvoker.getUrl();
        if (REGISTRY_PROTOCOL.equals(registryUrl.getProtocol())) {
            String protocol = registryUrl.getParameter(REGISTRY_KEY, DEFAULT_REGISTRY);
            registryUrl = registryUrl.setProtocol(protocol).removeParameter(REGISTRY_KEY);
        }
        return registryUrl;
    }

    private URL getRegisteredProviderUrl(final URL providerUrl, final URL registryUrl) {
        if (!registryUrl.getParameter(SIMPLIFIED_KEY, false)) {
            return providerUrl.removeParameters(getFilteredKeys(providerUrl)).removeParameters(MONITOR_KEY, BIND_IP_KEY, BIND_PORT_KEY, QOS_ENABLE, QOS_PORT, ACCEPT_FOREIGN_IP, VALIDATION_KEY, INTERFACES);
        } else {
            String extraKeys = registryUrl.getParameter(EXTRA_KEYS_KEY, "");
            if (!providerUrl.getPath().equals(providerUrl.getParameter(INTERFACE_KEY))) {
                if (StringUtils.isNotEmpty(extraKeys)) {
                    extraKeys += ",";
                }
                extraKeys += INTERFACE_KEY;
            }
            String[] paramsToRegistry = getParamsToRegistry(DEFAULT_REGISTER_PROVIDER_KEYS, COMMA_SPLIT_PATTERN.split(extraKeys));
            return URL.valueOf(providerUrl, paramsToRegistry, providerUrl.getParameter(METHODS_KEY, (String[]) null));
        }

    }

    private URL getSubscribedOverrideUrl(URL registeredProviderUrl) {
        return registeredProviderUrl.setProtocol(PROVIDER_PROTOCOL).addParameters(CATEGORY_KEY, CONFIGURATORS_CATEGORY, CHECK_KEY, String.valueOf(false));
    }

    private URL getProviderUrl(final Invoker<?> originInvoker) {
        String export = originInvoker.getUrl().getParameterAndDecoded(EXPORT_KEY);
        if (export == null || export.length() == 0) {
            throw new IllegalArgumentException("The registry export url is null! registry: " + originInvoker.getUrl());
        }
        return URL.valueOf(export);
    }

    private String getCacheKey(final Invoker<?> originInvoker) {
        URL providerUrl = getProviderUrl(originInvoker);
        String key = providerUrl.removeParameters("dynamic", "enabled").toFullString();
        return key;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
        // url: registry://127.0.0.1:2181/org.apache.dubbo.registry.RegistryService?application=dubbo-demo-api-consumer&dubbo=2.0.2&pid=6140&refer=application%3Ddubbo-demo-api-consumer%26dubbo%3D2.0.2%26interface%3Dorg.apache.dubbo.demo.DemoService%26lazy%3Dfalse%26methods%3DsayHello%26pid%3D6140%26register.ip%3D192.168.1.108%26revision%3D1.0.0%26side%3Dconsumer%26sticky%3Dfalse%26timestamp%3D1562837699631%26version%3D1.0.0&registry=zookeeper&timestamp=1562837701297
        logger.debug("url: " + url);
        // 取registry参数值，并将其设置为协议头
        url = URLBuilder.from(url).setProtocol(url.getParameter(REGISTRY_KEY, DEFAULT_REGISTRY)).removeParameter(REGISTRY_KEY).build();
        // url: zookeeper://127.0.0.1:2181/org.apache.dubbo.registry.RegistryService?application=dubbo-demo-api-consumer&dubbo=2.0.2&pid=6140&refer=application%3Ddubbo-demo-api-consumer%26dubbo%3D2.0.2%26interface%3Dorg.apache.dubbo.demo.DemoService%26lazy%3Dfalse%26methods%3DsayHello%26pid%3D6140%26register.ip%3D192.168.1.108%26revision%3D1.0.0%26side%3Dconsumer%26sticky%3Dfalse%26timestamp%3D1562837699631%26version%3D1.0.0&timestamp=1562837701297
        logger.debug("url: " + url);
        // 获取注册中心实例
        // ZookeeperRegistryFactory#getRegistry
        Registry registry = registryFactory.getRegistry(url);
        if (RegistryService.class.equals(type)) {
            return proxyFactory.getInvoker((T) registry, type, url);
        }
        // 将url查询字符串转为Map
        Map<String, String> qs = StringUtils.parseQueryString(url.getParameterAndDecoded(REFER_KEY));
        // qs: {side=consumer, application=dubbo-demo-api-consumer, register.ip=192.168.1.108, lazy=false, methods=sayHello, sticky=false, dubbo=2.0.2, pid=10472, interface=org.apache.dubbo.demo.DemoService, version=1.0.0, revision=1.0.0, timestamp=1562838295954}
        logger.debug("qs: " + qs);
        // 获取group配置
        String group = qs.get(GROUP_KEY);
        if (group != null && group.length() > 0) {
            if ((COMMA_SPLIT_PATTERN.split(group)).length > 1 || "*".equals(group)) {
                // 通过SPI加载MergeableCluster实例，并调用doRefer继续执行服务引用逻辑
                return doRefer(getMergeableCluster(), registry, type, url);
            }
        }
        // 调用doRefer继续执行服务引用逻辑
        return doRefer(cluster, registry, type, url);
    }

    private Cluster getMergeableCluster() {
        return ExtensionLoader.getExtensionLoader(Cluster.class).getExtension("mergeable");
    }

    private <T> Invoker<T> doRefer(Cluster cluster, Registry registry, Class<T> type, URL url) {
        // 创建RegistryDirectory实例
        RegistryDirectory<T> directory = new RegistryDirectory<T>(type, url);
        // 设置注册中心和协议
        directory.setRegistry(registry);
        // Protocol$Adaptive
        directory.setProtocol(protocol);
        Map<String, String> parameters = new HashMap<String, String>(directory.getUrl().getParameters());
        // 生成服务消费者链接
        URL subscribeUrl = new URL(CONSUMER_PROTOCOL, parameters.remove(REGISTER_IP_KEY), 0, type.getName(), parameters);
        // consumer://192.168.1.108/org.apache.dubbo.demo.DemoService:1.0.0?application=dubbo-demo-api-consumer&dubbo=2.0.2&interface=org.apache.dubbo.demo.DemoService&lazy=false&methods=sayHello&pid=1288&revision=1.0.0&side=consumer&sticky=false&timestamp=1562838404626&version=1.0.0
        logger.debug("subscribeUrl: " + subscribeUrl);
        // 注册服务消费者，在consumers目录下新节点
        if (!ANY_VALUE.equals(url.getServiceInterface()) && url.getParameter(REGISTER_KEY, true)) {
            directory.setRegisteredConsumerUrl(getRegisteredConsumerUrl(subscribeUrl, url));
            registry.register(directory.getRegisteredConsumerUrl());
        }
        directory.buildRouterChain(subscribeUrl);
        // 订阅providers、configurators、routers等节点数据
        directory.subscribe(subscribeUrl.addParameter(CATEGORY_KEY, PROVIDERS_CATEGORY + "," + CONFIGURATORS_CATEGORY + "," + ROUTERS_CATEGORY));
        // 一个注册中心可能有多个服务提供者，因此需要将多个服务提供者合并为一个
        Invoker invoker = cluster.join(directory);
        ProviderConsumerRegTable.registerConsumer(invoker, url, subscribeUrl, directory);
        return invoker;
    }

    public URL getRegisteredConsumerUrl(final URL consumerUrl, URL registryUrl) {
        if (!registryUrl.getParameter(SIMPLIFIED_KEY, false)) {
            return consumerUrl.addParameters(CATEGORY_KEY, CONSUMERS_CATEGORY, CHECK_KEY, String.valueOf(false));
        } else {
            return URL.valueOf(consumerUrl, DEFAULT_REGISTER_CONSUMER_KEYS, null).addParameters(CATEGORY_KEY, CONSUMERS_CATEGORY, CHECK_KEY, String.valueOf(false));
        }
    }

    public String[] getParamsToRegistry(String[] defaultKeys, String[] additionalParameterKeys) {
        int additionalLen = additionalParameterKeys.length;
        String[] registryParams = new String[defaultKeys.length + additionalLen];
        System.arraycopy(defaultKeys, 0, registryParams, 0, defaultKeys.length);
        System.arraycopy(additionalParameterKeys, 0, registryParams, defaultKeys.length, additionalLen);
        return registryParams;
    }

    @Override
    public void destroy() {
        List<Exporter<?>> exporters = new ArrayList<Exporter<?>>(bounds.values());
        for (Exporter<?> exporter : exporters) {
            exporter.unexport();
        }
        bounds.clear();
        DynamicConfiguration.getDynamicConfiguration().removeListener(ApplicationModel.getApplication() + CONFIGURATORS_SUFFIX, providerConfigurationListener);
    }

    private static URL getConfigedInvokerUrl(List<Configurator> configurators, URL url) {
        if (configurators != null && configurators.size() > 0) {
            for (Configurator configurator : configurators) {
                url = configurator.configure(url);
            }
        }
        return url;
    }

    public static class InvokerDelegate<T> extends InvokerWrapper<T> {
        private final Invoker<T> invoker;

        public InvokerDelegate(Invoker<T> invoker, URL url) {
            super(invoker, url);
            this.invoker = invoker;
        }

        public Invoker<T> getInvoker() {
            if (invoker instanceof InvokerDelegate) {
                return ((InvokerDelegate<T>) invoker).getInvoker();
            } else {
                return invoker;
            }
        }

    }

    static private class DestroyableExporter<T> implements Exporter<T> {

        private Exporter<T> exporter;

        public DestroyableExporter(Exporter<T> exporter) {
            this.exporter = exporter;
        }

        @Override
        public Invoker<T> getInvoker() {
            return exporter.getInvoker();
        }

        @Override
        public void unexport() {
            exporter.unexport();
        }

    }

    private class OverrideListener implements NotifyListener {
        private final URL subscribeUrl;

        private final Invoker originInvoker;

        private List<Configurator> configurators;

        public OverrideListener(URL subscribeUrl, Invoker originalInvoker) {
            this.subscribeUrl = subscribeUrl;
            this.originInvoker = originalInvoker;
        }

        @Override
        public synchronized void notify(List<URL> urls) {
            logger.debug("original override urls: " + urls);
            List<URL> matchedUrls = getMatchedUrls(urls, subscribeUrl.addParameter(CATEGORY_KEY, CONFIGURATORS_CATEGORY));
            logger.debug("subscribe url: " + subscribeUrl + ", override urls: " + matchedUrls);
            if (matchedUrls.isEmpty()) {
                return;
            }
            this.configurators = Configurator.toConfigurators(classifyUrls(matchedUrls, UrlUtils::isConfigurator)).orElse(configurators);
            doOverrideIfNecessary();
        }

        public synchronized void doOverrideIfNecessary() {
            final Invoker<?> invoker;
            if (originInvoker instanceof InvokerDelegate) {
                invoker = ((InvokerDelegate<?>) originInvoker).getInvoker();
            } else {
                invoker = originInvoker;
            }
            URL originUrl = RegistryProtocol.this.getProviderUrl(invoker);
            String key = getCacheKey(originInvoker);
            ExporterChangeableWrapper<?> exporter = bounds.get(key);
            if (exporter == null) {
                logger.warn(new IllegalStateException("error state, exporter should not be null"));
                return;
            }
            URL currentUrl = exporter.getInvoker().getUrl();
            URL newUrl = getConfigedInvokerUrl(configurators, originUrl);
            newUrl = getConfigedInvokerUrl(serviceConfigurationListeners.get(originUrl.getServiceKey()).getConfigurators(), newUrl);
            newUrl = getConfigedInvokerUrl(providerConfigurationListener.getConfigurators(), newUrl);
            if (!currentUrl.equals(newUrl)) {
                RegistryProtocol.this.reExport(originInvoker, newUrl);
                logger.info("exported provider url changed, origin url: " + originUrl + ", old export url: " + currentUrl + ", new export url: " + newUrl);
            }
        }

        private List<URL> getMatchedUrls(List<URL> configuratorUrls, URL currentSubscribe) {
            List<URL> result = new ArrayList<URL>();
            for (URL url : configuratorUrls) {
                URL overrideUrl = url;
                if (url.getParameter(CATEGORY_KEY) == null && OVERRIDE_PROTOCOL.equals(url.getProtocol())) {
                    overrideUrl = url.addParameter(CATEGORY_KEY, CONFIGURATORS_CATEGORY);
                }
                if (UrlUtils.isMatch(currentSubscribe, overrideUrl)) {
                    result.add(url);
                }
            }
            return result;
        }

    }

    private class ServiceConfigurationListener extends AbstractConfiguratorListener {
        private URL providerUrl;

        private OverrideListener notifyListener;

        public ServiceConfigurationListener(URL providerUrl, OverrideListener notifyListener) {
            this.providerUrl = providerUrl;
            this.notifyListener = notifyListener;
            this.initWith(providerUrl.getEncodedServiceKey() + CONFIGURATORS_SUFFIX);
        }

        private <T> URL overrideUrl(URL providerUrl) {
            return RegistryProtocol.getConfigedInvokerUrl(configurators, providerUrl);
        }

        @Override
        protected void notifyOverrides() {
            notifyListener.doOverrideIfNecessary();
        }

    }

    private class ProviderConfigurationListener extends AbstractConfiguratorListener {

        public ProviderConfigurationListener() {
            this.initWith(ApplicationModel.getApplication() + CONFIGURATORS_SUFFIX);
        }

        private <T> URL overrideUrl(URL providerUrl) {
            return RegistryProtocol.getConfigedInvokerUrl(configurators, providerUrl);
        }

        @Override
        protected void notifyOverrides() {
            overrideListeners.values().forEach(listener -> ((OverrideListener) listener).doOverrideIfNecessary());
        }

    }

    private class ExporterChangeableWrapper<T> implements Exporter<T> {

        private final ExecutorService executor = newSingleThreadExecutor(new NamedThreadFactory("Exporter-Unexport", true));

        private final Invoker<T> originInvoker;

        private Exporter<T> exporter;

        private URL subscribeUrl;

        private URL registerUrl;

        public ExporterChangeableWrapper(Exporter<T> exporter, Invoker<T> originInvoker) {
            this.exporter = exporter;
            this.originInvoker = originInvoker;
        }

        public Invoker<T> getOriginInvoker() {
            return originInvoker;
        }

        @Override
        public Invoker<T> getInvoker() {
            return exporter.getInvoker();
        }

        public void setExporter(Exporter<T> exporter) {
            this.exporter = exporter;
        }

        @Override
        public void unexport() {
            String key = getCacheKey(this.originInvoker);
            bounds.remove(key);
            Registry registry = RegistryProtocol.INSTANCE.getRegistry(originInvoker);
            try {
                registry.unregister(registerUrl);
            } catch (Throwable t) {
                logger.warn(t.getMessage(), t);
            }
            try {
                NotifyListener listener = RegistryProtocol.INSTANCE.overrideListeners.remove(subscribeUrl);
                registry.unsubscribe(subscribeUrl, listener);
                DynamicConfiguration.getDynamicConfiguration().removeListener(subscribeUrl.getServiceKey() + CONFIGURATORS_SUFFIX, serviceConfigurationListeners.get(subscribeUrl.getServiceKey()));
            } catch (Throwable t) {
                logger.warn(t.getMessage(), t);
            }
            executor.submit(() -> {
                try {
                    int timeout = ConfigurationUtils.getServerShutdownTimeout();
                    if (timeout > 0) {
                        logger.info("Waiting " + timeout + "ms for registry to notify all consumers before unexport. " + "Usually, this is called when you use dubbo API");
                        Thread.sleep(timeout);
                    }
                    exporter.unexport();
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
            });
        }

        public void setSubscribeUrl(URL subscribeUrl) {
            this.subscribeUrl = subscribeUrl;
        }

        public void setRegisterUrl(URL registerUrl) {
            this.registerUrl = registerUrl;
        }

    }

}
