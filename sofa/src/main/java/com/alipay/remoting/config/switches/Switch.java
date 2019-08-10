package com.alipay.remoting.config.switches;

public interface Switch {

    void turnOn(int index);

    void turnOff(int index);

    boolean isOn(int index);

}