package org.apache.dubbo.rpc.cluster.configurator;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.RemotingConstants;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.cluster.Configurator;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.apache.dubbo.common.constants.CommonConstants.*;
import static org.apache.dubbo.common.constants.RegistryConstants.*;
import static org.apache.dubbo.rpc.cluster.Constants.CONFIG_VERSION_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.OVERRIDE_PROVIDERS_KEY;

public abstract class AbstractConfigurator implements Configurator {

    private final URL configuratorUrl;

    public AbstractConfigurator(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("configurator url == null");
        }
        this.configuratorUrl = url;
    }

    @Override
    public URL getUrl() {
        return configuratorUrl;
    }

    @Override
    public URL configure(URL url) {
        if (!configuratorUrl.getParameter(ENABLED_KEY, true) || configuratorUrl.getHost() == null || url == null || url.getHost() == null) {
            return url;
        }
        String apiVersion = configuratorUrl.getParameter(CONFIG_VERSION_KEY);
        if (StringUtils.isNotEmpty(apiVersion)) {
            String currentSide = url.getParameter(SIDE_KEY);
            String configuratorSide = configuratorUrl.getParameter(SIDE_KEY);
            if (currentSide.equals(configuratorSide) && CONSUMER.equals(configuratorSide) && 0 == configuratorUrl.getPort()) {
                url = configureIfMatch(NetUtils.getLocalHost(), url);
            } else if (currentSide.equals(configuratorSide) && PROVIDER.equals(configuratorSide) && url.getPort() == configuratorUrl.getPort()) {
                url = configureIfMatch(url.getHost(), url);
            }
        } else {
            url = configureDeprecated(url);
        }
        return url;
    }

    @Deprecated
    private URL configureDeprecated(URL url) {
        if (configuratorUrl.getPort() != 0) {
            if (url.getPort() == configuratorUrl.getPort()) {
                return configureIfMatch(url.getHost(), url);
            }
        } else {
            if (url.getParameter(SIDE_KEY, PROVIDER).equals(CONSUMER)) {
                return configureIfMatch(NetUtils.getLocalHost(), url);
            } else if (url.getParameter(SIDE_KEY, CONSUMER).equals(PROVIDER)) {
                return configureIfMatch(ANYHOST_VALUE, url);
            }
        }
        return url;
    }

    private URL configureIfMatch(String host, URL url) {
        if (ANYHOST_VALUE.equals(configuratorUrl.getHost()) || host.equals(configuratorUrl.getHost())) {
            String providers = configuratorUrl.getParameter(OVERRIDE_PROVIDERS_KEY);
            if (StringUtils.isEmpty(providers) || providers.contains(url.getAddress()) || providers.contains(ANYHOST_VALUE)) {
                String configApplication = configuratorUrl.getParameter(APPLICATION_KEY, configuratorUrl.getUsername());
                String currentApplication = url.getParameter(APPLICATION_KEY, url.getUsername());
                if (configApplication == null || ANY_VALUE.equals(configApplication) || configApplication.equals(currentApplication)) {
                    Set<String> conditionKeys = new HashSet<String>();
                    conditionKeys.add(CATEGORY_KEY);
                    conditionKeys.add(RemotingConstants.CHECK_KEY);
                    conditionKeys.add(DYNAMIC_KEY);
                    conditionKeys.add(ENABLED_KEY);
                    conditionKeys.add(GROUP_KEY);
                    conditionKeys.add(VERSION_KEY);
                    conditionKeys.add(APPLICATION_KEY);
                    conditionKeys.add(SIDE_KEY);
                    conditionKeys.add(CONFIG_VERSION_KEY);
                    conditionKeys.add(COMPATIBLE_CONFIG_KEY);
                    for (Map.Entry<String, String> entry : configuratorUrl.getParameters().entrySet()) {
                        String key = entry.getKey();
                        String value = entry.getValue();
                        if (key.startsWith("~") || APPLICATION_KEY.equals(key) || SIDE_KEY.equals(key)) {
                            conditionKeys.add(key);
                            if (value != null && !ANY_VALUE.equals(value) && !value.equals(url.getParameter(key.startsWith("~") ? key.substring(1) : key))) {
                                return url;
                            }
                        }
                    }
                    return doConfigure(url, configuratorUrl.removeParameters(conditionKeys));
                }
            }
        }
        return url;
    }

    protected abstract URL doConfigure(URL currentUrl, URL configUrl);

}
