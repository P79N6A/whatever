package org.apache.dubbo.remoting;

public interface IdleSensible {

    default boolean canHandleIdle() {
        return false;
    }

}
