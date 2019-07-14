package org.apache.dubbo.common.utils;

public final class ArrayUtils {

    private ArrayUtils() {
    }

    public static boolean isEmpty(final Object[] array) {
        return array == null || array.length == 0;
    }

    public static boolean isNotEmpty(final Object[] array) {
        return !isEmpty(array);
    }

}
