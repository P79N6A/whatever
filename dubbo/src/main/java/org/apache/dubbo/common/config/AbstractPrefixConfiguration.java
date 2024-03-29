package org.apache.dubbo.common.config;

import org.apache.dubbo.common.utils.StringUtils;

public abstract class AbstractPrefixConfiguration implements Configuration {
    protected String id;

    protected String prefix;

    public AbstractPrefixConfiguration(String prefix, String id) {
        super();
        if (StringUtils.isNotEmpty(prefix) && !prefix.endsWith(".")) {
            this.prefix = prefix + ".";
        } else {
            this.prefix = prefix;
        }
        this.id = id;
    }

    @Override
    public Object getProperty(String key, Object defaultValue) {
        Object value = null;
        if (StringUtils.isNotEmpty(prefix) && StringUtils.isNotEmpty(id)) {
            value = getInternalProperty(prefix + id + "." + key);
        }
        if (value == null && StringUtils.isNotEmpty(prefix)) {
            value = getInternalProperty(prefix + key);
        }
        if (value == null) {
            value = getInternalProperty(key);
        }
        return value != null ? value : defaultValue;
    }

}
