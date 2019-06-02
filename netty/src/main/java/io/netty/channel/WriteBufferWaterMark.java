package io.netty.channel;

import static io.netty.util.internal.ObjectUtil.checkPositiveOrZero;

public final class WriteBufferWaterMark {

    private static final int DEFAULT_LOW_WATER_MARK = 32 * 1024;
    private static final int DEFAULT_HIGH_WATER_MARK = 64 * 1024;

    public static final WriteBufferWaterMark DEFAULT = new WriteBufferWaterMark(DEFAULT_LOW_WATER_MARK, DEFAULT_HIGH_WATER_MARK, false);

    private final int low;
    private final int high;

    public WriteBufferWaterMark(int low, int high) {
        this(low, high, true);
    }

    WriteBufferWaterMark(int low, int high, boolean validate) {
        if (validate) {
            checkPositiveOrZero(low, "low");
            if (high < low) {
                throw new IllegalArgumentException("write buffer's high water mark cannot be less than " + " low water mark (" + low + "): " + high);
            }
        }
        this.low = low;
        this.high = high;
    }

    public int low() {
        return low;
    }

    public int high() {
        return high;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(55).append("WriteBufferWaterMark(low: ").append(low).append(", high: ").append(high).append(")");
        return builder.toString();
    }

}
