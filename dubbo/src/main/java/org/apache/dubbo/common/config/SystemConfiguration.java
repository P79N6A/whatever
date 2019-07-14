package org.apache.dubbo.common.config;

public class SystemConfiguration extends AbstractPrefixConfiguration {

    public SystemConfiguration(String prefix, String id) {
        super(prefix, id);
    }

    public SystemConfiguration() {
        this(null, null);
    }

    @Override
    public Object getInternalProperty(String key) {
        return System.getProperty(key);
    }

}
