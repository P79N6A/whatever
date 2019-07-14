package org.apache.dubbo.common.config;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ConfigUtils;

public class PropertiesConfiguration extends AbstractPrefixConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(PropertiesConfiguration.class);

    public PropertiesConfiguration(String prefix, String id) {
        super(prefix, id);
    }

    public PropertiesConfiguration() {
        this(null, null);
    }

    @Override
    public Object getInternalProperty(String key) {
        return ConfigUtils.getProperty(key);
    }

}
