package com.alipay.remoting;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;

import java.util.concurrent.TimeUnit;

public class TimerHolder {

    private final static long defaultTickDuration = 10;

    private static class DefaultInstance {
        static final Timer INSTANCE = new HashedWheelTimer(new NamedThreadFactory("DefaultTimer" + defaultTickDuration, true), defaultTickDuration, TimeUnit.MILLISECONDS);

    }

    private TimerHolder() {
    }

    public static Timer getTimer() {
        return DefaultInstance.INSTANCE;
    }

}