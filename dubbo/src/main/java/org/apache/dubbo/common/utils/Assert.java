package org.apache.dubbo.common.utils;

public abstract class Assert {

    protected Assert() {
    }

    public static void notNull(Object obj, String message) {
        if (obj == null) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void notEmptyString(String str, String message) {
        if (StringUtils.isEmpty(str)) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void notNull(Object obj, RuntimeException exception) {
        if (obj == null) {
            throw exception;
        }
    }

}
