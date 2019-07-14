package org.apache.dubbo.rpc.cluster.merger;

import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.rpc.cluster.Merger;

public class DoubleArrayMerger implements Merger<double[]> {

    @Override
    public double[] merge(double[]... items) {
        if (ArrayUtils.isEmpty(items)) {
            return new double[0];
        }
        int total = 0;
        for (double[] array : items) {
            if (array != null) {
                total += array.length;
            }
        }
        double[] result = new double[total];
        int index = 0;
        for (double[] array : items) {
            if (array != null) {
                for (double item : array) {
                    result[index++] = item;
                }
            }
        }
        return result;
    }

}
