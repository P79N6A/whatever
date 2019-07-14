package org.apache.dubbo.rpc.cluster.merger;

import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.rpc.cluster.Merger;

public class LongArrayMerger implements Merger<long[]> {

    @Override
    public long[] merge(long[]... items) {
        if (ArrayUtils.isEmpty(items)) {
            return new long[0];
        }
        int total = 0;
        for (long[] array : items) {
            if (array != null) {
                total += array.length;
            }
        }
        long[] result = new long[total];
        int index = 0;
        for (long[] array : items) {
            if (array != null) {
                for (long item : array) {
                    result[index++] = item;
                }
            }
        }
        return result;
    }

}
