package com.alipay.remoting.config.configs;

public interface ConfigContainer {

    boolean contains(ConfigType configType, ConfigItem configItem);

    <T> T get(ConfigType configType, ConfigItem configItem);

    void set(ConfigType configType, ConfigItem configItem, Object value);

}