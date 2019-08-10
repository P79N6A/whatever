package com.alipay.remoting.config;

public class BoltOption<T> {

    private final String name;

    private T defaultValue;

    protected BoltOption(String name, T defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
    }

    public String name() {
        return name;
    }

    public T defaultValue() {
        return defaultValue;
    }

    public static <T> BoltOption<T> valueOf(String name) {
        return new BoltOption<T>(name, null);
    }

    public static <T> BoltOption<T> valueOf(String name, T defaultValue) {
        return new BoltOption<T>(name, defaultValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BoltOption<?> that = (BoltOption<?>) o;
        return name != null ? name.equals(that.name) : that.name == null;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

}
