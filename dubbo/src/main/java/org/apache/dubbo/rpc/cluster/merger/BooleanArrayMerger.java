package org.apache.dubbo.rpc.cluster.merger;

import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.rpc.cluster.Merger;

public class BooleanArrayMerger implements Merger<boolean[]> {

    @Override
    public boolean[] merge(boolean[]... items) {
        if (ArrayUtils.isEmpty(items)) {
            return new boolean[0];
        }
        int totalLen = 0;
        for (boolean[] array : items) {
            if (array != null) {
                totalLen += array.length;
            }
        }
        boolean[] result = new boolean[totalLen];
        int index = 0;
        for (boolean[] array : items) {
            if (array != null) {
                for (boolean item : array) {
                    result[index++] = item;
                }
            }
        }
        return result;
    }

}
