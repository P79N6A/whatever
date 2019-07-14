package org.apache.dubbo.rpc.cluster.merger;

import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.rpc.cluster.Merger;

public class CharArrayMerger implements Merger<char[]> {

    @Override
    public char[] merge(char[]... items) {
        if (ArrayUtils.isEmpty(items)) {
            return new char[0];
        }
        int total = 0;
        for (char[] array : items) {
            if (array != null) {
                total += array.length;
            }
        }
        char[] result = new char[total];
        int index = 0;
        for (char[] array : items) {
            if (array != null) {
                for (char item : array) {
                    result[index++] = item;
                }
            }
        }
        return result;
    }

}
