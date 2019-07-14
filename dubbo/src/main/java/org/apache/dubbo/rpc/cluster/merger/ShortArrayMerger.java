package org.apache.dubbo.rpc.cluster.merger;

import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.rpc.cluster.Merger;

public class ShortArrayMerger implements Merger<short[]> {

    @Override
    public short[] merge(short[]... items) {
        if (ArrayUtils.isEmpty(items)) {
            return new short[0];
        }
        int total = 0;
        for (short[] array : items) {
            if (array != null) {
                total += array.length;
            }
        }
        short[] result = new short[total];
        int index = 0;
        for (short[] array : items) {
            if (array != null) {
                for (short item : array) {
                    result[index++] = item;
                }
            }
        }
        return result;
    }

}
