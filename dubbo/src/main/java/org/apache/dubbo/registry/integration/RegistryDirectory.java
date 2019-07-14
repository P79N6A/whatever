package org.apache.dubbo.registry.integration;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.Version;
import org.apache.dubbo.common.constants.RemotingConstants;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.*;
import org.apache.dubbo.configcenter.DynamicConfiguration;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.*;
import org.apache.dubbo.rpc.cluster.directory.AbstractDirectory;
import org.apache.dubbo.rpc.cluster.directory.StaticDirectory;
import org.apache.dubbo.rpc.cluster.support.ClusterUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.protocol.InvokerWrapper;

import java.util.*;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.CommonConstants.*;
import static org.apache.dubbo.common.constants.ConfigConstants.DUBBO_PROTOCOL;
import static org.apache.dubbo.common.constants.ConfigConstants.REFER_KEY;
import static org.apache.dubbo.common.constants.MonitorConstants.MONITOR_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.*;

/**
 * 动态服务目录，实现了NotifyListener接口
 * 当注册中心服务配置发生变化后，RegistryDirectory可收到与当前服务相关的变化
 * 收到变更通知后，RegistryDirectory可根据配置变更信息刷新Invoker列表
 */
public class RegistryDirectory<T> extends AbstractDirectory<T> implements NotifyListener {

    private static final Logger logger = LoggerFactory.getLogger(RegistryDirectory.class);

    private static final Cluster CLUSTER = ExtensionLoader.getExtensionLoader(Cluster.class).getAdaptiveExtension();

    private static final RouterFactory ROUTER_FACTORY = ExtensionLoader.getExtensionLoader(RouterFactory.class).getAdaptiveExtension();

    private final String serviceKey;

    private final Class<T> serviceType;

    private final Map<String, String> queryMap;

    private final URL directoryUrl;

    private final boolean multiGroup;

    private Protocol protocol;

    private Registry registry;

    private volatile boolean forbidden = false;

    private volatile URL overrideDirectoryUrl;

    private volatile URL registeredConsumerUrl;

    private volatile List<Configurator> configurators;

    private volatile Map<String, Invoker<T>> urlInvokerMap;

    private volatile List<Invoker<T>> invokers;

    private volatile Set<URL> cachedInvokerUrls;

    private static final ConsumerConfigurationListener CONSUMER_CONFIGURATION_LISTENER = new ConsumerConfigurationListener();

    private ReferenceConfigurationListener serviceConfigurationListener;

    public RegistryDirectory(Class<T> serviceType, URL url) {
        super(url);
        if (serviceType == null) {
            throw new IllegalArgumentException("service type is null.");
        }
        if (url.getServiceKey() == null || url.getServiceKey().length() == 0) {
            throw new IllegalArgumentException("registry serviceKey is null.");
        }
        this.serviceType = serviceType;
        this.serviceKey = url.getServiceKey();
        this.queryMap = StringUtils.parseQueryString(url.getParameterAndDecoded(REFER_KEY));
        this.overrideDirectoryUrl = this.directoryUrl = turnRegistryUrlToConsumerUrl(url);
        String group = directoryUrl.getParameter(GROUP_KEY, "");
        this.multiGroup = group != null && (ANY_VALUE.equals(group) || group.contains(","));
    }

    private URL turnRegistryUrlToConsumerUrl(URL url) {
        String isDefault = url.getParameter(DEFAULT_KEY);
        if (StringUtils.isNotEmpty(isDefault)) {
            queryMap.put(REGISTRY_KEY + "." + DEFAULT_KEY, isDefault);
        }
        return URLBuilder.from(url).setPath(url.getServiceInterface()).clearParameters().addParameters(queryMap).removeParameter(MONITOR_KEY).build();
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

    public void subscribe(URL url) {
        setConsumerUrl(url);
        CONSUMER_CONFIGURATION_LISTENER.addNotifyListener(this);
        serviceConfigurationListener = new ReferenceConfigurationListener(this, url);
        registry.subscribe(url, this);
    }

    @Override
    public void destroy() {
        if (isDestroyed()) {
            return;
        }
        try {
            if (getRegisteredConsumerUrl() != null && registry != null && registry.isAvailable()) {
                registry.unregister(getRegisteredConsumerUrl());
            }
        } catch (Throwable t) {
            logger.warn("unexpected error when unregister service " + serviceKey + "from registry" + registry.getUrl(), t);
        }
        try {
            if (getConsumerUrl() != null && registry != null && registry.isAvailable()) {
                registry.unsubscribe(getConsumerUrl(), this);
            }
            DynamicConfiguration.getDynamicConfiguration().removeListener(ApplicationModel.getApplication(), CONSUMER_CONFIGURATION_LISTENER);
        } catch (Throwable t) {
            logger.warn("unexpected error when unsubscribe service " + serviceKey + "from registry" + registry.getUrl(), t);
        }
        super.destroy();
        try {
            destroyAllInvokers();
        } catch (Throwable t) {
            logger.warn("Failed to destroy service " + serviceKey, t);
        }
    }

    @Override
    public synchronized void notify(List<URL> urls) {

        logger.debug("urls: " + urls);

        // 三个集合，分别用于存放服务提供者url，路由url，配置器url
        // 根据category参数将url分别放到不同的列表中
        Map<String, List<URL>> categoryUrls = urls.stream().filter(Objects::nonNull).filter(this::isValidCategory).filter(this::isNotCompatibleFor26x).collect(Collectors.groupingBy(url -> {
            if (UrlUtils.isConfigurator(url)) {
                // 添加配置器url
                return CONFIGURATORS_CATEGORY;
            } else if (UrlUtils.isRoute(url)) {
                // 添加路由器url
                return ROUTERS_CATEGORY;
            } else if (UrlUtils.isProvider(url)) {
                // 添加服务提供者url
                return PROVIDERS_CATEGORY;
            }
            // 忽略不支持的category
            return "";
        }));
        List<URL> configuratorURLs = categoryUrls.getOrDefault(CONFIGURATORS_CATEGORY, Collections.emptyList());
        // 将url转成Configurator
        this.configurators = Configurator.toConfigurators(configuratorURLs).orElse(this.configurators);
        List<URL> routerURLs = categoryUrls.getOrDefault(ROUTERS_CATEGORY, Collections.emptyList());
        // 将url转成Router
        toRouters(routerURLs).ifPresent(this::addRouters);
        List<URL> providerURLs = categoryUrls.getOrDefault(PROVIDERS_CATEGORY, Collections.emptyList());
        // 刷新Invoker列表
        refreshOverrideAndInvoker(providerURLs);
    }

    private void refreshOverrideAndInvoker(List<URL> urls) {
        overrideDirectoryUrl();
        refreshInvoker(urls);
    }

    private void refreshInvoker(List<URL> invokerUrls) {
        Assert.notNull(invokerUrls, "invokerUrls should not be null");
        // invokerUrls仅有一个元素，且url协议头为empty，此时表示禁用所有服务
        if (invokerUrls.size() == 1 && invokerUrls.get(0) != null && EMPTY_PROTOCOL.equals(invokerUrls.get(0).getProtocol())) {
            // 设置forbidden为true
            this.forbidden = true;
            this.invokers = Collections.emptyList();
            routerChain.setInvokers(this.invokers);
            // 销毁所有Invoker
            destroyAllInvokers();
        } else {
            this.forbidden = false;
            Map<String, Invoker<T>> oldUrlInvokerMap = this.urlInvokerMap;
            if (invokerUrls == Collections.<URL>emptyList()) {
                invokerUrls = new ArrayList<>();
            }
            if (invokerUrls.isEmpty() && this.cachedInvokerUrls != null) {
                // 添加缓存url到invokerUrls中
                invokerUrls.addAll(this.cachedInvokerUrls);
            } else {
                this.cachedInvokerUrls = new HashSet<>();
                // 缓存invokerUrls
                this.cachedInvokerUrls.addAll(invokerUrls);
            }
            if (invokerUrls.isEmpty()) {
                return;
            }
            // [
            // dubbo://192.168.1.108:20880/org.apache.dubbo.demo.DemoService:1.0.0?anyhost=true
            // &application=dubbo-demo-api-provider
            // &deprecated=false
            // &dubbo=2.0.2
            // &dynamic=true
            // &generic=false
            // &interface=org.apache.dubbo.demo.DemoService
            // &methods=sayHello
            // &pid=8564
            // &register=true
            // &release=
            // &revision=1.0.0
            // &sayHello.timeout=1000
            // &side=provider
            // &timestamp=1562927529007
            // &version=1.0.0
            // ]
            logger.debug("invokerUrls: " + invokerUrls);
            // 将url转成Invoker
            Map<String, Invoker<T>> newUrlInvokerMap = toInvokers(invokerUrls);
            // 转换出错，直接打印异常，并返回
            if (CollectionUtils.isEmptyMap(newUrlInvokerMap)) {
                logger.error(new IllegalStateException("urls to invokers error .invokerUrls.size :" + invokerUrls.size() + ", invoker.size :0. urls :" + invokerUrls.toString()));
                return;
            }
            List<Invoker<T>> newInvokers = Collections.unmodifiableList(new ArrayList<>(newUrlInvokerMap.values()));
            routerChain.setInvokers(newInvokers);
            // 合并多个组的Invoker
            this.invokers = multiGroup ? toMergeInvokerList(newInvokers) : newInvokers;
            this.urlInvokerMap = newUrlInvokerMap;
            try {
                // 销毁无用Invoker
                destroyUnusedInvokers(oldUrlInvokerMap, newUrlInvokerMap);
            } catch (Exception e) {
                logger.warn("destroyUnusedInvokers error. ", e);
            }
        }
    }

    private List<Invoker<T>> toMergeInvokerList(List<Invoker<T>> invokers) {
        List<Invoker<T>> mergedInvokers = new ArrayList<>();
        Map<String, List<Invoker<T>>> groupMap = new HashMap<>();
        for (Invoker<T> invoker : invokers) {
            String group = invoker.getUrl().getParameter(GROUP_KEY, "");
            groupMap.computeIfAbsent(group, k -> new ArrayList<>());
            groupMap.get(group).add(invoker);
        }
        if (groupMap.size() == 1) {
            mergedInvokers.addAll(groupMap.values().iterator().next());
        } else if (groupMap.size() > 1) {
            for (List<Invoker<T>> groupList : groupMap.values()) {
                StaticDirectory<T> staticDirectory = new StaticDirectory<>(groupList);
                staticDirectory.buildRouterChain();
                mergedInvokers.add(CLUSTER.join(staticDirectory));
            }
        } else {
            mergedInvokers = invokers;
        }
        return mergedInvokers;
    }

    private Optional<List<Router>> toRouters(List<URL> urls) {
        if (urls == null || urls.isEmpty()) {
            return Optional.empty();
        }
        List<Router> routers = new ArrayList<>();
        for (URL url : urls) {
            if (EMPTY_PROTOCOL.equals(url.getProtocol())) {
                continue;
            }
            String routerType = url.getParameter(ROUTER_KEY);
            if (routerType != null && routerType.length() > 0) {
                url = url.setProtocol(routerType);
            }
            try {
                Router router = ROUTER_FACTORY.getRouter(url);
                if (!routers.contains(router)) {
                    routers.add(router);
                }
            } catch (Throwable t) {
                logger.error("convert router url to router error, url: " + url, t);
            }
        }
        return Optional.of(routers);
    }

    private Map<String, Invoker<T>> toInvokers(List<URL> urls) {
        Map<String, Invoker<T>> newUrlInvokerMap = new HashMap<>();
        if (urls == null || urls.isEmpty()) {
            return newUrlInvokerMap;
        }
        Set<String> keys = new HashSet<>();
        // 获取服务消费端配置的协议
        String queryProtocols = this.queryMap.get(PROTOCOL_KEY);
        for (URL providerUrl : urls) {
            if (queryProtocols != null && queryProtocols.length() > 0) {
                boolean accept = false;
                String[] acceptProtocols = queryProtocols.split(",");
                // 检测服务提供者协议是否被服务消费者所支持
                for (String acceptProtocol : acceptProtocols) {
                    if (providerUrl.getProtocol().equals(acceptProtocol)) {
                        accept = true;
                        break;
                    }
                }
                if (!accept) {
                    // 若服务消费者协议头不被消费者所支持，则忽略当前providerUrl
                    continue;
                }
            }
            // 忽略empty协议
            if (EMPTY_PROTOCOL.equals(providerUrl.getProtocol())) {
                continue;
            }
            // 通过SPI检测服务端协议是否被消费端支持，不支持则抛出异常
            if (!ExtensionLoader.getExtensionLoader(Protocol.class).hasExtension(providerUrl.getProtocol())) {
                logger.error(new IllegalStateException("Unsupported protocol " + providerUrl.getProtocol() + " in notified url: " + providerUrl + " from registry " + getUrl().getAddress() + " to consumer " + NetUtils.getLocalHost() + ", supported protocol: " + ExtensionLoader.getExtensionLoader(Protocol.class).getSupportedExtensions()));
                continue;
            }
            // 合并url
            URL url = mergeUrl(providerUrl);
            String key = url.toFullString();
            if (keys.contains(key)) {
                // 忽略重复url
                continue;
            }
            keys.add(key);
            // 将本地Invoker缓存赋值给localUrlInvokerMap
            Map<String, Invoker<T>> localUrlInvokerMap = this.urlInvokerMap;
            // 获取与url对应的Invoker
            Invoker<T> invoker = localUrlInvokerMap == null ? null : localUrlInvokerMap.get(key);
            // 缓存未命中
            if (invoker == null) {
                try {
                    boolean enabled = true;
                    if (url.hasParameter(DISABLED_KEY)) {
                        // 获取disable配置，取反，然后赋值给enable变量
                        enabled = !url.getParameter(DISABLED_KEY, false);
                    } else {
                        // 获取enable配置，并赋值给enable变量
                        enabled = url.getParameter(ENABLED_KEY, true);
                    }
                    if (enabled) {
                        // 调用refer获取Invoker
                        invoker = new InvokerDelegate<>(protocol.refer(serviceType, url), url, providerUrl);
                    }
                } catch (Throwable t) {
                    logger.error("Failed to refer invoker for interface:" + serviceType + ",url:(" + url + ")" + t.getMessage(), t);
                }
                if (invoker != null) {
                    // 缓存Invoker实例
                    newUrlInvokerMap.put(key, invoker);
                }
            }
            // 缓存命中
            else {
                // 将invoker存储到newUrlInvokerMap中
                newUrlInvokerMap.put(key, invoker);
            }
        }
        keys.clear();
        return newUrlInvokerMap;
    }

    private URL mergeUrl(URL providerUrl) {
        providerUrl = ClusterUtils.mergeUrl(providerUrl, queryMap);
        providerUrl = overrideWithConfigurator(providerUrl);
        providerUrl = providerUrl.addParameter(RemotingConstants.CHECK_KEY, String.valueOf(false));
        this.overrideDirectoryUrl = this.overrideDirectoryUrl.addParametersIfAbsent(providerUrl.getParameters());
        if ((providerUrl.getPath() == null || providerUrl.getPath().length() == 0) && DUBBO_PROTOCOL.equals(providerUrl.getProtocol())) {
            String path = directoryUrl.getParameter(INTERFACE_KEY);
            if (path != null) {
                int i = path.indexOf('/');
                if (i >= 0) {
                    path = path.substring(i + 1);
                }
                i = path.lastIndexOf(':');
                if (i >= 0) {
                    path = path.substring(0, i);
                }
                providerUrl = providerUrl.setPath(path);
            }
        }
        return providerUrl;
    }

    private URL overrideWithConfigurator(URL providerUrl) {
        providerUrl = overrideWithConfigurators(this.configurators, providerUrl);
        providerUrl = overrideWithConfigurators(CONSUMER_CONFIGURATION_LISTENER.getConfigurators(), providerUrl);
        if (serviceConfigurationListener != null) {
            providerUrl = overrideWithConfigurators(serviceConfigurationListener.getConfigurators(), providerUrl);
        }
        return providerUrl;
    }

    private URL overrideWithConfigurators(List<Configurator> configurators, URL url) {
        if (CollectionUtils.isNotEmpty(configurators)) {
            for (Configurator configurator : configurators) {
                url = configurator.configure(url);
            }
        }
        return url;
    }

    private void destroyAllInvokers() {
        Map<String, Invoker<T>> localUrlInvokerMap = this.urlInvokerMap;
        if (localUrlInvokerMap != null) {
            for (Invoker<T> invoker : new ArrayList<>(localUrlInvokerMap.values())) {
                try {
                    invoker.destroy();
                } catch (Throwable t) {
                    logger.warn("Failed to destroy service " + serviceKey + " to provider " + invoker.getUrl(), t);
                }
            }
            localUrlInvokerMap.clear();
        }
        invokers = null;
    }

    private void destroyUnusedInvokers(Map<String, Invoker<T>> oldUrlInvokerMap, Map<String, Invoker<T>> newUrlInvokerMap) {
        if (newUrlInvokerMap == null || newUrlInvokerMap.size() == 0) {
            destroyAllInvokers();
            return;
        }
        List<String> deleted = null;
        if (oldUrlInvokerMap != null) {
            // 获取新生成的Invoker列表
            Collection<Invoker<T>> newInvokers = newUrlInvokerMap.values();
            // 遍历老的 <url, Invoker> 映射表
            for (Map.Entry<String, Invoker<T>> entry : oldUrlInvokerMap.entrySet()) {
                // 检测newInvokers中是否包含老的Invoker
                if (!newInvokers.contains(entry.getValue())) {
                    if (deleted == null) {
                        deleted = new ArrayList<>();
                    }
                    // 若不包含，则将老的Invoker对应的url存deleted列表中
                    deleted.add(entry.getKey());
                }
            }
        }
        if (deleted != null) {
            // 遍历deleted集合，并到老的 <url, Invoker> 映射关系表查出Invoker，销毁之
            for (String url : deleted) {
                if (url != null) {
                    // 从oldUrlInvokerMap中移除url对应的Invoker
                    Invoker<T> invoker = oldUrlInvokerMap.remove(url);
                    if (invoker != null) {
                        try {
                            // 销毁Invoker
                            invoker.destroy();
                            if (logger.isDebugEnabled()) {
                                logger.debug("destroy invoker[" + invoker.getUrl() + "] success. ");
                            }
                        } catch (Exception e) {
                            logger.warn("destroy invoker[" + invoker.getUrl() + "] failed. " + e.getMessage(), e);
                        }
                    }
                }
            }
        }
    }

    @Override
    public List<Invoker<T>> doList(Invocation invocation) {
        if (forbidden) {
            // 服务提供者关闭或禁用了服务，此时抛出异常
            throw new RpcException(RpcException.FORBIDDEN_EXCEPTION, "No provider available from registry " + getUrl().getAddress() + " for service " + getConsumerUrl().getServiceKey() + " on consumer " + NetUtils.getLocalHost() + " use dubbo version " + Version.getVersion() + ", please check status of providers(disabled, not registered or in blacklist).");
        }
        if (multiGroup) {
            return this.invokers == null ? Collections.emptyList() : this.invokers;
        }
        List<Invoker<T>> invokers = null;
        try {
            invokers = routerChain.route(getConsumerUrl(), invocation);
        } catch (Throwable t) {
            logger.error("Failed to execute router: " + getUrl() + ", cause: " + t.getMessage(), t);
        }
        return invokers == null ? Collections.emptyList() : invokers;
    }

    @Override
    public Class<T> getInterface() {
        return serviceType;
    }

    @Override
    public URL getUrl() {
        return this.overrideDirectoryUrl;
    }

    public URL getRegisteredConsumerUrl() {
        return registeredConsumerUrl;
    }

    public void setRegisteredConsumerUrl(URL registeredConsumerUrl) {
        this.registeredConsumerUrl = registeredConsumerUrl;
    }

    @Override
    public boolean isAvailable() {
        if (isDestroyed()) {
            return false;
        }
        Map<String, Invoker<T>> localUrlInvokerMap = urlInvokerMap;
        if (localUrlInvokerMap != null && localUrlInvokerMap.size() > 0) {
            for (Invoker<T> invoker : new ArrayList<>(localUrlInvokerMap.values())) {
                if (invoker.isAvailable()) {
                    return true;
                }
            }
        }
        return false;
    }

    public void buildRouterChain(URL url) {
        this.setRouterChain(RouterChain.buildChain(url));
    }

    public Map<String, Invoker<T>> getUrlInvokerMap() {
        return urlInvokerMap;
    }

    public List<Invoker<T>> getInvokers() {
        return invokers;
    }

    private boolean isValidCategory(URL url) {
        String category = url.getParameter(CATEGORY_KEY, DEFAULT_CATEGORY);
        if ((ROUTERS_CATEGORY.equals(category) || ROUTE_PROTOCOL.equals(url.getProtocol())) || PROVIDERS_CATEGORY.equals(category) || CONFIGURATORS_CATEGORY.equals(category) || DYNAMIC_CONFIGURATORS_CATEGORY.equals(category) || APP_DYNAMIC_CONFIGURATORS_CATEGORY.equals(category)) {
            return true;
        }
        logger.warn("Unsupported category " + category + " in notified url: " + url + " from registry " + getUrl().getAddress() + " to consumer " + NetUtils.getLocalHost());
        return false;
    }

    private boolean isNotCompatibleFor26x(URL url) {
        return StringUtils.isEmpty(url.getParameter(COMPATIBLE_CONFIG_KEY));
    }

    private void overrideDirectoryUrl() {
        this.overrideDirectoryUrl = directoryUrl;
        List<Configurator> localConfigurators = this.configurators;
        doOverrideUrl(localConfigurators);
        List<Configurator> localAppDynamicConfigurators = CONSUMER_CONFIGURATION_LISTENER.getConfigurators();
        doOverrideUrl(localAppDynamicConfigurators);
        if (serviceConfigurationListener != null) {
            List<Configurator> localDynamicConfigurators = serviceConfigurationListener.getConfigurators();
            doOverrideUrl(localDynamicConfigurators);
        }
    }

    private void doOverrideUrl(List<Configurator> configurators) {
        if (CollectionUtils.isNotEmpty(configurators)) {
            for (Configurator configurator : configurators) {
                this.overrideDirectoryUrl = configurator.configure(overrideDirectoryUrl);
            }
        }
    }

    private static class InvokerDelegate<T> extends InvokerWrapper<T> {
        private URL providerUrl;

        public InvokerDelegate(Invoker<T> invoker, URL url, URL providerUrl) {
            super(invoker, url);
            this.providerUrl = providerUrl;
        }

        public URL getProviderUrl() {
            return providerUrl;
        }

    }

    private static class ReferenceConfigurationListener extends AbstractConfiguratorListener {
        private RegistryDirectory directory;

        private URL url;

        ReferenceConfigurationListener(RegistryDirectory directory, URL url) {
            this.directory = directory;
            this.url = url;
            this.initWith(url.getEncodedServiceKey() + CONFIGURATORS_SUFFIX);
        }

        @Override
        protected void notifyOverrides() {
            directory.refreshInvoker(Collections.emptyList());
        }

    }

    private static class ConsumerConfigurationListener extends AbstractConfiguratorListener {
        List<RegistryDirectory> listeners = new ArrayList<>();

        ConsumerConfigurationListener() {
            this.initWith(ApplicationModel.getApplication() + CONFIGURATORS_SUFFIX);
        }

        void addNotifyListener(RegistryDirectory listener) {
            this.listeners.add(listener);
        }

        @Override
        protected void notifyOverrides() {
            listeners.forEach(listener -> listener.refreshInvoker(Collections.emptyList()));
        }

    }

}
