package org.apache.dubbo.rpc.filter.tps;

import java.util.concurrent.atomic.LongAdder;

class StatItem {

    private String name;

    private long lastResetTime;

    private long interval;

    private LongAdder token;

    private int rate;

    StatItem(String name, int rate, long interval) {
        this.name = name;
        this.rate = rate;
        this.interval = interval;
        this.lastResetTime = System.currentTimeMillis();
        this.token = buildLongAdder(rate);
    }

    public boolean isAllowable() {
        long now = System.currentTimeMillis();
        if (now > lastResetTime + interval) {
            token = buildLongAdder(rate);
            lastResetTime = now;
        }
        if (token.sum() < 0) {
            return false;
        }
        token.decrement();
        return true;
    }

    public long getInterval() {
        return interval;
    }

    public int getRate() {
        return rate;
    }

    long getLastResetTime() {
        return lastResetTime;
    }

    long getToken() {
        return token.sum();
    }

    @Override
    public String toString() {
        return new StringBuilder(32).append("StatItem ").append("[name=").append(name).append(", ").append("rate = ").append(rate).append(", ").append("interval = ").append(interval).append("]").toString();
    }

    private LongAdder buildLongAdder(int rate) {
        LongAdder adder = new LongAdder();
        adder.add(rate);
        return adder;
    }

}
