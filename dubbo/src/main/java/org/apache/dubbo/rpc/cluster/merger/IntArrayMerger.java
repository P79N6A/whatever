package org.apache.dubbo.rpc.cluster.merger;

import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.rpc.cluster.Merger;

public class IntArrayMerger implements Merger<int[]> {

    @Override
    public int[] merge(int[]... items) {
        if (ArrayUtils.isEmpty(items)) {
            return new int[0];
        }
        int totalLen = 0;
        for (int[] array : items) {
            if (array != null) {
                totalLen += array.length;
            }
        }
        int[] result = new int[totalLen];
        int index = 0;
        for (int[] array : items) {
            if (array != null) {
                for (int item : array) {
                    result[index++] = item;
                }
            }
        }
        return result;
    }

}
