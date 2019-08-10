package com.alipay.remoting.config.switches;

import com.alipay.remoting.config.ConfigManager;

import java.util.BitSet;

public class GlobalSwitch implements Switch {

    public static final int CONN_RECONNECT_SWITCH = 0;

    public static final int CONN_MONITOR_SWITCH = 1;

    public static final int SERVER_MANAGE_CONNECTION_SWITCH = 2;

    public static final int SERVER_SYNC_STOP = 3;

    /**
     * 位数组
     */
    private BitSet userSettings = new BitSet();

    public GlobalSwitch() {
        // 第一个位
        if (ConfigManager.conn_reconnect_switch()) {
            userSettings.set(CONN_RECONNECT_SWITCH);
        } else {
            userSettings.clear(CONN_RECONNECT_SWITCH);
        }
        // 第二个位
        if (ConfigManager.conn_monitor_switch()) {
            userSettings.set(CONN_MONITOR_SWITCH);
        } else {
            userSettings.clear(CONN_MONITOR_SWITCH);
        }
    }

    @Override
    public void turnOn(int index) {
        // 将指定索引处的位设置为true
        this.userSettings.set(index);
    }

    @Override
    public void turnOff(int index) {
        // 将索引指定处的位设置为false
        this.userSettings.clear(index);
    }

    @Override
    public boolean isOn(int index) {
        return this.userSettings.get(index);
    }

    public static void main(String args[]) {
        BitSet bits1 = new BitSet(16);
        BitSet bits2 = new BitSet(16);
        // set some bits
        for (int i = 0; i < 16; i++) {
            if ((i % 2) == 0)
                bits1.set(i);
            if ((i % 5) != 0)
                bits2.set(i);
        }
        System.out.println("Initial pattern in bits1: ");
        System.out.println(bits1); // {0, 2, 4, 6, 8, 10, 12, 14}
        System.out.println("\nInitial pattern in bits2: ");
        System.out.println(bits2); // {1, 2, 3, 4, 6, 7, 8, 9, 11, 12, 13, 14}
        // AND bits
        bits2.and(bits1);
        System.out.println("\nbits2 AND bits1: ");
        System.out.println(bits2); // {2, 4, 6, 8, 12, 14}
        // OR bits
        bits2.or(bits1);
        System.out.println("\nbits2 OR bits1: ");
        System.out.println(bits2); // {0, 2, 4, 6, 8, 10, 12, 14}
        // XOR bits
        bits2.xor(bits1);
        System.out.println("\nbits2 XOR bits1: ");
        System.out.println(bits2); // {}
    }

}