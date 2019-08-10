package com.alipay.remoting.config;

public interface Configurable {

    <T> T option(BoltOption<T> option);

    <T> Configurable option(BoltOption<T> option, T value);

}
