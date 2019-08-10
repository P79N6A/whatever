package com.alipay.remoting.config;

import com.alipay.remoting.config.configs.ConfigContainer;
import com.alipay.remoting.config.configs.NettyConfigure;
import com.alipay.remoting.config.switches.GlobalSwitch;

public interface ConfigurableInstance extends NettyConfigure {

    ConfigContainer conf();

    GlobalSwitch switches();

}