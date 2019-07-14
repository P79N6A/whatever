package org.apache.dubbo.common.config;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class Environment {
    private static final Environment INSTANCE = new Environment();

    private Map<String, PropertiesConfiguration> propertiesConfigs = new ConcurrentHashMap<>();

    private Map<String, SystemConfiguration> systemConfigs = new ConcurrentHashMap<>();

    private Map<String, EnvironmentConfiguration> environmentConfigs = new ConcurrentHashMap<>();

    private Map<String, InmemoryConfiguration> externalConfigs = new ConcurrentHashMap<>();

    private Map<String, InmemoryConfiguration> appExternalConfigs = new ConcurrentHashMap<>();

    private Map<String, String> externalConfigurationMap = new HashMap<>();

    private Map<String, String> appExternalConfigurationMap = new HashMap<>();

    private boolean configCenterFirst = true;

    private Configuration dynamicConfiguration;

    public static Environment getInstance() {
        return INSTANCE;
    }

    public PropertiesConfiguration getPropertiesConfig(String prefix, String id) {
        return propertiesConfigs.computeIfAbsent(toKey(prefix, id), k -> new PropertiesConfiguration(prefix, id));
    }

    public SystemConfiguration getSystemConfig(String prefix, String id) {
        return systemConfigs.computeIfAbsent(toKey(prefix, id), k -> new SystemConfiguration(prefix, id));
    }

    public InmemoryConfiguration getExternalConfig(String prefix, String id) {
        return externalConfigs.computeIfAbsent(toKey(prefix, id), k -> {
            InmemoryConfiguration configuration = new InmemoryConfiguration(prefix, id);
            configuration.setProperties(externalConfigurationMap);
            return configuration;
        });
    }

    public InmemoryConfiguration getAppExternalConfig(String prefix, String id) {
        return appExternalConfigs.computeIfAbsent(toKey(prefix, id), k -> {
            InmemoryConfiguration configuration = new InmemoryConfiguration(prefix, id);
            configuration.setProperties(appExternalConfigurationMap);
            return configuration;
        });
    }

    public EnvironmentConfiguration getEnvironmentConfig(String prefix, String id) {
        return environmentConfigs.computeIfAbsent(toKey(prefix, id), k -> new EnvironmentConfiguration(prefix, id));
    }

    public void setExternalConfigMap(Map<String, String> externalConfiguration) {
        this.externalConfigurationMap = externalConfiguration;
    }

    public void setAppExternalConfigMap(Map<String, String> appExternalConfiguration) {
        this.appExternalConfigurationMap = appExternalConfiguration;
    }

    public Map<String, String> getExternalConfigurationMap() {
        return externalConfigurationMap;
    }

    public Map<String, String> getAppExternalConfigurationMap() {
        return appExternalConfigurationMap;
    }

    public void updateExternalConfigurationMap(Map<String, String> externalMap) {
        this.externalConfigurationMap.putAll(externalMap);
    }

    public void updateAppExternalConfigurationMap(Map<String, String> externalMap) {
        this.appExternalConfigurationMap.putAll(externalMap);
    }

    public CompositeConfiguration getConfiguration(String prefix, String id) {
        CompositeConfiguration compositeConfiguration = new CompositeConfiguration();
        compositeConfiguration.addConfiguration(this.getSystemConfig(prefix, id));
        compositeConfiguration.addConfiguration(this.getAppExternalConfig(prefix, id));
        compositeConfiguration.addConfiguration(this.getExternalConfig(prefix, id));
        compositeConfiguration.addConfiguration(this.getPropertiesConfig(prefix, id));
        return compositeConfiguration;
    }

    public Configuration getConfiguration() {
        return getConfiguration(null, null);
    }

    private static String toKey(String prefix, String id) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotEmpty(prefix)) {
            sb.append(prefix);
        }
        if (StringUtils.isNotEmpty(id)) {
            sb.append(id);
        }
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '.') {
            sb.append(".");
        }
        if (sb.length() > 0) {
            return sb.toString();
        }
        return CommonConstants.DUBBO;
    }

    public boolean isConfigCenterFirst() {
        return configCenterFirst;
    }

    public void setConfigCenterFirst(boolean configCenterFirst) {
        this.configCenterFirst = configCenterFirst;
    }

    public Optional<Configuration> getDynamicConfiguration() {
        return Optional.ofNullable(dynamicConfiguration);
    }

    public void setDynamicConfiguration(Configuration dynamicConfiguration) {
        this.dynamicConfiguration = dynamicConfiguration;
    }

    public void clearExternalConfigs() {
        this.externalConfigs.clear();
        this.externalConfigurationMap.clear();
    }

    public void clearAppExternalConfigs() {
        this.appExternalConfigs.clear();
        this.appExternalConfigurationMap.clear();
    }

}
