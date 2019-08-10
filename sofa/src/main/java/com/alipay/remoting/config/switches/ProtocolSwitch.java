package com.alipay.remoting.config.switches;

import java.util.BitSet;

public class ProtocolSwitch implements Switch {

    public static final int CRC_SWITCH_INDEX = 0x000;

    public static final boolean CRC_SWITCH_DEFAULT_VALUE = true;

    private BitSet bs = new BitSet();

    @Override
    public void turnOn(int index) {
        this.bs.set(index);
    }

    @Override
    public void turnOff(int index) {
        this.bs.clear(index);
    }

    @Override
    public boolean isOn(int index) {
        return this.bs.get(index);
    }

    public byte toByte() {
        return toByte(this.bs);
    }

    public static boolean isOn(int switchIndex, int value) {
        return toBitSet(value).get(switchIndex);
    }

    public static ProtocolSwitch create(int value) {
        ProtocolSwitch status = new ProtocolSwitch();
        status.setBs(toBitSet(value));
        return status;
    }

    public static ProtocolSwitch create(int[] index) {
        ProtocolSwitch status = new ProtocolSwitch();
        for (int i = 0; i < index.length; ++i) {
            status.turnOn(index[i]);
        }
        return status;
    }

    public static byte toByte(BitSet bs) {
        int value = 0;
        for (int i = 0; i < bs.length(); ++i) {
            if (bs.get(i)) {
                value += 1 << i;
            }
        }
        if (bs.length() > 7) {
            throw new IllegalArgumentException("The byte value " + value + " generated according to bit set " + bs + " is out of range, should be limited between [" + Byte.MIN_VALUE + "] to [" + Byte.MAX_VALUE + "]");
        }
        return (byte) value;
    }

    public static BitSet toBitSet(int value) {
        if (value > Byte.MAX_VALUE || value < Byte.MIN_VALUE) {
            throw new IllegalArgumentException("The value " + value + " is out of byte range, should be limited between [" + Byte.MIN_VALUE + "] to [" + Byte.MAX_VALUE + "]");
        }
        BitSet bs = new BitSet();
        int index = 0;
        while (value != 0) {
            if (value % 2 != 0) {
                bs.set(index);
            }
            ++index;
            value = (byte) (value >> 1);
        }
        return bs;
    }

    public void setBs(BitSet bs) {
        this.bs = bs;
    }

}