package org.apache.dubbo.common.utils;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.RemotingConstants;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.CommonConstants.*;
import static org.apache.dubbo.common.constants.ConfigConstants.*;
import static org.apache.dubbo.common.constants.RegistryConstants.*;

public class UrlUtils {

    /**
     * in the url string,mark the param begin
     */
    private final static String URL_PARAM_STARTING_SYMBOL = "?";

    public static URL parseURL(String address, Map<String, String> defaults) {
        if (address == null || address.length() == 0) {
            return null;
        }
        String url;
        if (address.contains("://") || address.contains(URL_PARAM_STARTING_SYMBOL)) {
            url = address;
        } else {
            String[] addresses = COMMA_SPLIT_PATTERN.split(address);
            url = addresses[0];
            if (addresses.length > 1) {
                StringBuilder backup = new StringBuilder();
                for (int i = 1; i < addresses.length; i++) {
                    if (i > 1) {
                        backup.append(",");
                    }
                    backup.append(addresses[i]);
                }
                url += URL_PARAM_STARTING_SYMBOL + RemotingConstants.BACKUP_KEY + "=" + backup.toString();
            }
        }
        String defaultProtocol = defaults == null ? null : defaults.get(PROTOCOL_KEY);
        if (defaultProtocol == null || defaultProtocol.length() == 0) {
            defaultProtocol = DUBBO_PROTOCOL;
        }
        String defaultUsername = defaults == null ? null : defaults.get(USERNAME_KEY);
        String defaultPassword = defaults == null ? null : defaults.get(PASSWORD_KEY);
        int defaultPort = StringUtils.parseInteger(defaults == null ? null : defaults.get(PORT_KEY));
        String defaultPath = defaults == null ? null : defaults.get(PATH_KEY);
        Map<String, String> defaultParameters = defaults == null ? null : new HashMap<String, String>(defaults);
        if (defaultParameters != null) {
            defaultParameters.remove(PROTOCOL_KEY);
            defaultParameters.remove(USERNAME_KEY);
            defaultParameters.remove(PASSWORD_KEY);
            defaultParameters.remove(HOST_KEY);
            defaultParameters.remove(PORT_KEY);
            defaultParameters.remove(PATH_KEY);
        }
        URL u = URL.valueOf(url);
        boolean changed = false;
        String protocol = u.getProtocol();
        String username = u.getUsername();
        String password = u.getPassword();
        String host = u.getHost();
        int port = u.getPort();
        String path = u.getPath();
        Map<String, String> parameters = new HashMap<String, String>(u.getParameters());
        if ((protocol == null || protocol.length() == 0) && defaultProtocol != null && defaultProtocol.length() > 0) {
            changed = true;
            protocol = defaultProtocol;
        }
        if ((username == null || username.length() == 0) && defaultUsername != null && defaultUsername.length() > 0) {
            changed = true;
            username = defaultUsername;
        }
        if ((password == null || password.length() == 0) && defaultPassword != null && defaultPassword.length() > 0) {
            changed = true;
            password = defaultPassword;
        }
        /*if (u.isAnyHost() || u.isLocalHost()) {
            changed = true;
            host = NetUtils.getLocalHost();
        }*/
        if (port <= 0) {
            if (defaultPort > 0) {
                changed = true;
                port = defaultPort;
            } else {
                changed = true;
                port = 9090;
            }
        }
        if (path == null || path.length() == 0) {
            if (defaultPath != null && defaultPath.length() > 0) {
                changed = true;
                path = defaultPath;
            }
        }
        if (defaultParameters != null && defaultParameters.size() > 0) {
            for (Map.Entry<String, String> entry : defaultParameters.entrySet()) {
                String key = entry.getKey();
                String defaultValue = entry.getValue();
                if (defaultValue != null && defaultValue.length() > 0) {
                    String value = parameters.get(key);
                    if (StringUtils.isEmpty(value)) {
                        changed = true;
                        parameters.put(key, defaultValue);
                    }
                }
            }
        }
        if (changed) {
            u = new URL(protocol, username, password, host, port, path, parameters);
        }
        return u;
    }

    public static List<URL> parseURLs(String address, Map<String, String> defaults) {
        if (address == null || address.length() == 0) {
            return null;
        }
        String[] addresses = REGISTRY_SPLIT_PATTERN.split(address);
        if (addresses == null || addresses.length == 0) {
            return null; //here won't be empty
        }
        List<URL> registries = new ArrayList<URL>();
        for (String addr : addresses) {
            registries.add(parseURL(addr, defaults));
        }
        return registries;
    }

    public static Map<String, Map<String, String>> convertRegister(Map<String, Map<String, String>> register) {
        Map<String, Map<String, String>> newRegister = new HashMap<String, Map<String, String>>();
        for (Map.Entry<String, Map<String, String>> entry : register.entrySet()) {
            String serviceName = entry.getKey();
            Map<String, String> serviceUrls = entry.getValue();
            if (!serviceName.contains(":") && !serviceName.contains("/")) {
                for (Map.Entry<String, String> entry2 : serviceUrls.entrySet()) {
                    String serviceUrl = entry2.getKey();
                    String serviceQuery = entry2.getValue();
                    Map<String, String> params = StringUtils.parseQueryString(serviceQuery);
                    String group = params.get("group");
                    String version = params.get("version");
                    //params.remove("group");
                    //params.remove("version");
                    String name = serviceName;
                    if (group != null && group.length() > 0) {
                        name = group + "/" + name;
                    }
                    if (version != null && version.length() > 0) {
                        name = name + ":" + version;
                    }
                    Map<String, String> newUrls = newRegister.get(name);
                    if (newUrls == null) {
                        newUrls = new HashMap<String, String>();
                        newRegister.put(name, newUrls);
                    }
                    newUrls.put(serviceUrl, StringUtils.toQueryString(params));
                }
            } else {
                newRegister.put(serviceName, serviceUrls);
            }
        }
        return newRegister;
    }

    public static Map<String, String> convertSubscribe(Map<String, String> subscribe) {
        Map<String, String> newSubscribe = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : subscribe.entrySet()) {
            String serviceName = entry.getKey();
            String serviceQuery = entry.getValue();
            if (!serviceName.contains(":") && !serviceName.contains("/")) {
                Map<String, String> params = StringUtils.parseQueryString(serviceQuery);
                String group = params.get("group");
                String version = params.get("version");
                //params.remove("group");
                //params.remove("version");
                String name = serviceName;
                if (group != null && group.length() > 0) {
                    name = group + "/" + name;
                }
                if (version != null && version.length() > 0) {
                    name = name + ":" + version;
                }
                newSubscribe.put(name, StringUtils.toQueryString(params));
            } else {
                newSubscribe.put(serviceName, serviceQuery);
            }
        }
        return newSubscribe;
    }

    public static Map<String, Map<String, String>> revertRegister(Map<String, Map<String, String>> register) {
        Map<String, Map<String, String>> newRegister = new HashMap<String, Map<String, String>>();
        for (Map.Entry<String, Map<String, String>> entry : register.entrySet()) {
            String serviceName = entry.getKey();
            Map<String, String> serviceUrls = entry.getValue();
            if (serviceName.contains(":") || serviceName.contains("/")) {
                for (Map.Entry<String, String> entry2 : serviceUrls.entrySet()) {
                    String serviceUrl = entry2.getKey();
                    String serviceQuery = entry2.getValue();
                    Map<String, String> params = StringUtils.parseQueryString(serviceQuery);
                    String name = serviceName;
                    int i = name.indexOf('/');
                    if (i >= 0) {
                        params.put("group", name.substring(0, i));
                        name = name.substring(i + 1);
                    }
                    i = name.lastIndexOf(':');
                    if (i >= 0) {
                        params.put("version", name.substring(i + 1));
                        name = name.substring(0, i);
                    }
                    Map<String, String> newUrls = newRegister.get(name);
                    if (newUrls == null) {
                        newUrls = new HashMap<String, String>();
                        newRegister.put(name, newUrls);
                    }
                    newUrls.put(serviceUrl, StringUtils.toQueryString(params));
                }
            } else {
                newRegister.put(serviceName, serviceUrls);
            }
        }
        return newRegister;
    }

    public static Map<String, String> revertSubscribe(Map<String, String> subscribe) {
        Map<String, String> newSubscribe = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : subscribe.entrySet()) {
            String serviceName = entry.getKey();
            String serviceQuery = entry.getValue();
            if (serviceName.contains(":") || serviceName.contains("/")) {
                Map<String, String> params = StringUtils.parseQueryString(serviceQuery);
                String name = serviceName;
                int i = name.indexOf('/');
                if (i >= 0) {
                    params.put("group", name.substring(0, i));
                    name = name.substring(i + 1);
                }
                i = name.lastIndexOf(':');
                if (i >= 0) {
                    params.put("version", name.substring(i + 1));
                    name = name.substring(0, i);
                }
                newSubscribe.put(name, StringUtils.toQueryString(params));
            } else {
                newSubscribe.put(serviceName, serviceQuery);
            }
        }
        return newSubscribe;
    }

    public static Map<String, Map<String, String>> revertNotify(Map<String, Map<String, String>> notify) {
        if (notify != null && notify.size() > 0) {
            Map<String, Map<String, String>> newNotify = new HashMap<String, Map<String, String>>();
            for (Map.Entry<String, Map<String, String>> entry : notify.entrySet()) {
                String serviceName = entry.getKey();
                Map<String, String> serviceUrls = entry.getValue();
                if (!serviceName.contains(":") && !serviceName.contains("/")) {
                    if (serviceUrls != null && serviceUrls.size() > 0) {
                        for (Map.Entry<String, String> entry2 : serviceUrls.entrySet()) {
                            String url = entry2.getKey();
                            String query = entry2.getValue();
                            Map<String, String> params = StringUtils.parseQueryString(query);
                            String group = params.get("group");
                            String version = params.get("version");
                            // params.remove("group");
                            // params.remove("version");
                            String name = serviceName;
                            if (group != null && group.length() > 0) {
                                name = group + "/" + name;
                            }
                            if (version != null && version.length() > 0) {
                                name = name + ":" + version;
                            }
                            Map<String, String> newUrls = newNotify.get(name);
                            if (newUrls == null) {
                                newUrls = new HashMap<String, String>();
                                newNotify.put(name, newUrls);
                            }
                            newUrls.put(url, StringUtils.toQueryString(params));
                        }
                    }
                } else {
                    newNotify.put(serviceName, serviceUrls);
                }
            }
            return newNotify;
        }
        return notify;
    }

    //compatible for dubbo-2.0.0
    public static List<String> revertForbid(List<String> forbid, Set<URL> subscribed) {
        if (CollectionUtils.isNotEmpty(forbid)) {
            List<String> newForbid = new ArrayList<String>();
            for (String serviceName : forbid) {
                if (!serviceName.contains(":") && !serviceName.contains("/")) {
                    for (URL url : subscribed) {
                        if (serviceName.equals(url.getServiceInterface())) {
                            newForbid.add(url.getServiceKey());
                            break;
                        }
                    }
                } else {
                    newForbid.add(serviceName);
                }
            }
            return newForbid;
        }
        return forbid;
    }

    public static URL getEmptyUrl(String service, String category) {
        String group = null;
        String version = null;
        int i = service.indexOf('/');
        if (i > 0) {
            group = service.substring(0, i);
            service = service.substring(i + 1);
        }
        i = service.lastIndexOf(':');
        if (i > 0) {
            version = service.substring(i + 1);
            service = service.substring(0, i);
        }
        return URL.valueOf(EMPTY_PROTOCOL + "://0.0.0.0/" + service + URL_PARAM_STARTING_SYMBOL + CATEGORY_KEY + "=" + category + (group == null ? "" : "&" + GROUP_KEY + "=" + group) + (version == null ? "" : "&" + VERSION_KEY + "=" + version));
    }

    public static boolean isMatchCategory(String category, String categories) {
        if (categories == null || categories.length() == 0) {
            return DEFAULT_CATEGORY.equals(category);
        } else if (categories.contains(ANY_VALUE)) {
            return true;
        } else if (categories.contains(REMOVE_VALUE_PREFIX)) {
            return !categories.contains(REMOVE_VALUE_PREFIX + category);
        } else {
            return categories.contains(category);
        }
    }

    public static boolean isMatch(URL consumerUrl, URL providerUrl) {
        String consumerInterface = consumerUrl.getServiceInterface();
        String providerInterface = providerUrl.getServiceInterface();
        // FIXME accept providerUrl with '*' as interface name, after carefully thought about all possible scenarios I think it's ok to add this condition.
        // 比较服务接口是否匹配

        if (!(ANY_VALUE.equals(consumerInterface) || ANY_VALUE.equals(providerInterface) || StringUtils.isEquals(consumerInterface, providerInterface))) {
            return false;
        }
        if (!isMatchCategory(providerUrl.getParameter(CATEGORY_KEY, DEFAULT_CATEGORY), consumerUrl.getParameter(CATEGORY_KEY, DEFAULT_CATEGORY))) {
            return false;
        }
        // 判断provider是否被禁用服务
        if (!providerUrl.getParameter(ENABLED_KEY, true) && !ANY_VALUE.equals(consumerUrl.getParameter(ENABLED_KEY))) {
            return false;
        }
        String consumerGroup = consumerUrl.getParameter(GROUP_KEY);
        String consumerVersion = consumerUrl.getParameter(VERSION_KEY);
        String consumerClassifier = consumerUrl.getParameter(CLASSIFIER_KEY, ANY_VALUE);
        String providerGroup = providerUrl.getParameter(GROUP_KEY);
        String providerVersion = providerUrl.getParameter(VERSION_KEY);
        String providerClassifier = providerUrl.getParameter(CLASSIFIER_KEY, ANY_VALUE);
        // 匹配规则依赖于group、version、classifier三个属性
        // cond1. 判断消费者分组是否可以消费任何组|| 消费者和生产者组是否相等 || 消费者组包括生产者组
        // cond2. 判断消费者版本是否为* || 判断消费者组是否等于生产者组
        // cond3. 消费者分类是否为空 ||消费者分类是否为* || 消费者分类是否与生产者分类相等
        return (ANY_VALUE.equals(consumerGroup) || StringUtils.isEquals(consumerGroup, providerGroup) || StringUtils.isContains(consumerGroup, providerGroup)) && (ANY_VALUE.equals(consumerVersion) || StringUtils.isEquals(consumerVersion, providerVersion)) && (consumerClassifier == null || ANY_VALUE.equals(consumerClassifier) || StringUtils.isEquals(consumerClassifier, providerClassifier));
    }

    public static boolean isMatchGlobPattern(String pattern, String value, URL param) {
        if (param != null && pattern.startsWith("$")) {
            // 引用服务消费者参数，param 参数为服务消费者 url
            pattern = param.getRawParameter(pattern.substring(1));
        }
        // 调用重载方法继续比较
        return isMatchGlobPattern(pattern, value);
    }

    public static boolean isMatchGlobPattern(String pattern, String value) {
        // 对 * 通配符提供支持
        if ("*".equals(pattern)) {
            // 匹配规则为通配符 *，直接返回 true 即可
            return true;
        }
        if (StringUtils.isEmpty(pattern) && StringUtils.isEmpty(value)) {
            // pattern 和 value 均为空，此时可认为两者相等，返回 true
            return true;
        }
        if (StringUtils.isEmpty(pattern) || StringUtils.isEmpty(value)) {
            // pattern 和 value 其中有一个为空，表明两者不相等，返回 false
            return false;
        }
        // 定位 * 通配符位置
        int i = pattern.lastIndexOf('*');
        // doesn't find "*"
        if (i == -1) {
            // 匹配规则中不包含通配符，此时直接比较 value 和 pattern 是否相等即可，并返回比较结果
            return value.equals(pattern);
        }
        // 通配符 "*" 在匹配规则尾部，比如 10.0.21.*
        // "*" is at the end
        else if (i == pattern.length() - 1) {
            // 检测 value 是否以“不含通配符的匹配规则”开头，并返回结果。比如:
            // pattern = 10.0.21.*，value = 10.0.21.12，此时返回 true
            return value.startsWith(pattern.substring(0, i));
        }
        // 通配符 "*" 在匹配规则头部
        // "*" is at the beginning
        else if (i == 0) {
            // 检测 value 是否以“不含通配符的匹配规则”结尾，并返回结果
            return value.endsWith(pattern.substring(i + 1));
        }
        // 通配符 "*" 在匹配规则中间位置
        // "*" is in the middle
        else {
            // 通过通配符将 pattern 分成两半，得到 prefix 和 suffix
            String prefix = pattern.substring(0, i);
            String suffix = pattern.substring(i + 1);
            // 检测 value 是否以 prefix 开头，且以 suffix 结尾，并返回结果
            return value.startsWith(prefix) && value.endsWith(suffix);
        }
    }

    public static boolean isServiceKeyMatch(URL pattern, URL value) {
        return pattern.getParameter(INTERFACE_KEY).equals(value.getParameter(INTERFACE_KEY)) && isItemMatch(pattern.getParameter(GROUP_KEY), value.getParameter(GROUP_KEY)) && isItemMatch(pattern.getParameter(VERSION_KEY), value.getParameter(VERSION_KEY));
    }

    public static List<URL> classifyUrls(List<URL> urls, Predicate<URL> predicate) {
        return urls.stream().filter(predicate).collect(Collectors.toList());
    }

    public static boolean isConfigurator(URL url) {
        return OVERRIDE_PROTOCOL.equals(url.getProtocol()) || CONFIGURATORS_CATEGORY.equals(url.getParameter(CATEGORY_KEY, DEFAULT_CATEGORY));
    }

    public static boolean isRoute(URL url) {
        return ROUTE_PROTOCOL.equals(url.getProtocol()) || ROUTERS_CATEGORY.equals(url.getParameter(CATEGORY_KEY, DEFAULT_CATEGORY));
    }

    public static boolean isProvider(URL url) {
        return !OVERRIDE_PROTOCOL.equals(url.getProtocol()) && !ROUTE_PROTOCOL.equals(url.getProtocol()) && PROVIDERS_CATEGORY.equals(url.getParameter(CATEGORY_KEY, PROVIDERS_CATEGORY));
    }

    public static int getHeartbeat(URL url) {
        return url.getParameter(RemotingConstants.HEARTBEAT_KEY, RemotingConstants.DEFAULT_HEARTBEAT);
    }

    public static int getIdleTimeout(URL url) {
        int heartBeat = getHeartbeat(url);
        int idleTimeout = url.getParameter(RemotingConstants.HEARTBEAT_TIMEOUT_KEY, heartBeat * 3);
        if (idleTimeout < heartBeat * 2) {
            throw new IllegalStateException("idleTimeout < heartbeatInterval * 2");
        }
        return idleTimeout;
    }

    /**
     * Check if the given value matches the given pattern. The pattern supports wildcard "*".
     *
     * @param pattern pattern
     * @param value   value
     * @return true if match otherwise false
     */
    static boolean isItemMatch(String pattern, String value) {
        if (pattern == null) {
            return value == null;
        } else {
            return "*".equals(pattern) || pattern.equals(value);
        }
    }

}
