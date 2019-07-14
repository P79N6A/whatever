package org.apache.dubbo.rpc.cluster.merger;

import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.rpc.cluster.Merger;

public class ByteArrayMerger implements Merger<byte[]> {

    @Override
    public byte[] merge(byte[]... items) {
        if (ArrayUtils.isEmpty(items)) {
            return new byte[0];
        }
        int total = 0;
        for (byte[] array : items) {
            if (array != null) {
                total += array.length;
            }
        }
        byte[] result = new byte[total];
        int index = 0;
        for (byte[] array : items) {
            if (array != null) {
                for (byte item : array) {
                    result[index++] = item;
                }
            }
        }
        return result;
    }

}
