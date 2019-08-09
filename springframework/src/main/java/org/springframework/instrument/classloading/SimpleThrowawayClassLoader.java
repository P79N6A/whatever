package org.springframework.instrument.classloading;

import org.springframework.core.OverridingClassLoader;
import org.springframework.lang.Nullable;

public class SimpleThrowawayClassLoader extends OverridingClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    public SimpleThrowawayClassLoader(@Nullable ClassLoader parent) {
        super(parent);
    }

}
