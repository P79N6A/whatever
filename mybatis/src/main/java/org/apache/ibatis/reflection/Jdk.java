package org.apache.ibatis.reflection;

import org.apache.ibatis.io.Resources;

public class Jdk {

    @Deprecated
    public static final boolean parameterExists;

    static {
        boolean available = false;
        try {
            Resources.classForName("java.lang.reflect.Parameter");
            available = true;
        } catch (ClassNotFoundException e) {

        }
        parameterExists = available;
    }

    @Deprecated
    public static final boolean dateAndTimeApiExists;

    static {
        boolean available = false;
        try {
            Resources.classForName("java.time.Clock");
            available = true;
        } catch (ClassNotFoundException e) {

        }
        dateAndTimeApiExists = available;
    }

    @Deprecated
    public static final boolean optionalExists;

    static {
        boolean available = false;
        try {
            Resources.classForName("java.util.Optional");
            available = true;
        } catch (ClassNotFoundException e) {

        }
        optionalExists = available;
    }

    private Jdk() {
        super();
    }
}
