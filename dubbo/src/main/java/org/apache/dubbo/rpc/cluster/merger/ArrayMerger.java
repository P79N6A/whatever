package org.apache.dubbo.rpc.cluster.merger;

import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.rpc.cluster.Merger;

import java.lang.reflect.Array;

public class ArrayMerger implements Merger<Object[]> {

    public static final ArrayMerger INSTANCE = new ArrayMerger();

    @Override
    public Object[] merge(Object[]... items) {
        if (ArrayUtils.isEmpty(items)) {
            return new Object[0];
        }
        int i = 0;
        while (i < items.length && items[i] == null) {
            i++;
        }
        if (i == items.length) {
            return new Object[0];
        }
        Class<?> type = items[i].getClass().getComponentType();
        int totalLen = 0;
        for (; i < items.length; i++) {
            if (items[i] == null) {
                continue;
            }
            Class<?> itemType = items[i].getClass().getComponentType();
            if (itemType != type) {
                throw new IllegalArgumentException("Arguments' types are different");
            }
            totalLen += items[i].length;
        }
        if (totalLen == 0) {
            return new Object[0];
        }
        Object result = Array.newInstance(type, totalLen);
        int index = 0;
        for (Object[] array : items) {
            if (array != null) {
                for (int j = 0; j < array.length; j++) {
                    Array.set(result, index++, array[j]);
                }
            }
        }
        return (Object[]) result;
    }

}
