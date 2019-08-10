package com.alipay.remoting.config.configs;

import com.alipay.remoting.log.BoltLoggerFactory;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class DefaultConfigContainer implements ConfigContainer {

    private static final Logger logger = BoltLoggerFactory.getLogger("CommonDefault");

    private Map<ConfigType, Map<ConfigItem, Object>> userConfigs = new HashMap<ConfigType, Map<ConfigItem, Object>>();

    @Override
    public boolean contains(ConfigType configType, ConfigItem configItem) {
        validate(configType, configItem);
        return null != userConfigs.get(configType) && userConfigs.get(configType).containsKey(configItem);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(ConfigType configType, ConfigItem configItem) {
        validate(configType, configItem);
        if (userConfigs.containsKey(configType)) {
            return (T) userConfigs.get(configType).get(configItem);
        }
        return null;
    }

    @Override
    public void set(ConfigType configType, ConfigItem configItem, Object value) {
        validate(configType, configItem, value);
        Map<ConfigItem, Object> items = userConfigs.get(configType);
        if (null == items) {
            items = new HashMap<ConfigItem, Object>();
            userConfigs.put(configType, items);
        }
        Object prev = items.put(configItem, value);
        if (null != prev) {
            logger.warn("the value of ConfigType {}, ConfigItem {} changed from {} to {}", configType, configItem, prev.toString(), value.toString());
        }
    }

    private void validate(ConfigType configType, ConfigItem configItem) {
        if (null == configType || null == configItem) {
            throw new IllegalArgumentException(String.format("ConfigType {%s}, ConfigItem {%s} should not be null!", configType, configItem));
        }
    }

    private void validate(ConfigType configType, ConfigItem configItem, Object value) {
        if (null == configType || null == configItem || null == value) {
            throw new IllegalArgumentException(String.format("ConfigType {%s}, ConfigItem {%s}, value {%s} should not be null!", configType, configItem, value == null ? null : value.toString()));
        }
    }

}