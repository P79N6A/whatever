package org.apache.ibatis.reflection;

import java.util.Optional;

@Deprecated
public abstract class OptionalUtil {

    public static Object ofNullable(Object value) {
        return Optional.ofNullable(value);
    }

    private OptionalUtil() {
        super();
    }
}
