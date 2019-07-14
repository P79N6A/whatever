package org.apache.dubbo.rpc.cluster.merger;

import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.rpc.cluster.Merger;

public class FloatArrayMerger implements Merger<float[]> {

    @Override
    public float[] merge(float[]... items) {
        if (ArrayUtils.isEmpty(items)) {
            return new float[0];
        }
        int total = 0;
        for (float[] array : items) {
            if (array != null) {
                total += array.length;
            }
        }
        float[] result = new float[total];
        int index = 0;
        for (float[] array : items) {
            if (array != null) {
                for (float item : array) {
                    result[index++] = item;
                }
            }
        }
        return result;
    }

}
