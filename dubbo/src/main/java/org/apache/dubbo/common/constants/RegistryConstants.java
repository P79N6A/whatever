package org.apache.dubbo.common.constants;

public interface RegistryConstants {
    String REGISTER_KEY = "register";

    String SUBSCRIBE_KEY = "subscribe";

    String REGISTRY_KEY = "registry";

    String DEFAULT_REGISTRY = "dubbo";

    String REGISTRY_PROTOCOL = "registry";

    String DYNAMIC_KEY = "dynamic";

    String REGISTER = "register";

    String UNREGISTER = "unregister";

    String SUBSCRIBE = "subscribe";

    String UNSUBSCRIBE = "unsubscribe";

    String CATEGORY_KEY = "category";

    String PROVIDERS_CATEGORY = "providers";

    String CONSUMERS_CATEGORY = "consumers";

    String ROUTERS_CATEGORY = "routers";

    String DYNAMIC_ROUTERS_CATEGORY = "dynamicrouters";

    String DEFAULT_CATEGORY = PROVIDERS_CATEGORY;

    String CONFIGURATORS_CATEGORY = "configurators";

    String DYNAMIC_CONFIGURATORS_CATEGORY = "dynamicconfigurators";

    String APP_DYNAMIC_CONFIGURATORS_CATEGORY = "appdynamicconfigurators";

    String CONFIGURATORS_SUFFIX = ".configurators";

    String ROUTERS_SUFFIX = ".routers";

    String TRACE_PROTOCOL = "trace";

    String EMPTY_PROTOCOL = "empty";

    String ADMIN_PROTOCOL = "admin";

    String PROVIDER_PROTOCOL = "provider";

    String CONSUMER_PROTOCOL = "consumer";

    String ROUTE_PROTOCOL = "route";

    String SCRIPT_PROTOCOL = "script";

    String CONDITION_PROTOCOL = "condition";

    String SIMPLIFIED_KEY = "simplified";

    String EXTRA_KEYS_KEY = "extra-keys";

    String OVERRIDE_PROTOCOL = "override";

    String COMPATIBLE_CONFIG_KEY = "compatible_config";

    String REGISTRY_FILESAVE_SYNC_KEY = "save.file";

    String REGISTRY_RETRY_PERIOD_KEY = "retry.period";

    String REGISTRY_RETRY_TIMES_KEY = "retry.times";

    int DEFAULT_REGISTRY_RETRY_PERIOD = 5 * 1000;

    int DEFAULT_REGISTRY_RETRY_TIMES = 3;

    String REGISTRY_RECONNECT_PERIOD_KEY = "reconnect.period";

    int DEFAULT_REGISTRY_RECONNECT_PERIOD = 3 * 1000;

    String SESSION_TIMEOUT_KEY = "session";

    int DEFAULT_SESSION_TIMEOUT = 60 * 1000;

    String ROUTER_KEY = "router";

}
