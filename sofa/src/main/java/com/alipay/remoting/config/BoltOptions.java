package com.alipay.remoting.config;

import java.util.concurrent.ConcurrentHashMap;

public class BoltOptions {

    private ConcurrentHashMap<BoltOption<?>, Object> options = new ConcurrentHashMap<BoltOption<?>, Object>();

    @SuppressWarnings("unchecked")
    public <T> T option(BoltOption<T> option) {
        Object value = options.get(option);
        if (value == null) {
            value = option.defaultValue();
        }
        return value == null ? null : (T) value;
    }

    public <T> BoltOptions option(BoltOption<T> option, T value) {
        if (value == null) {
            options.remove(option);
            return this;
        }
        options.put(option, value);
        return this;
    }

}
