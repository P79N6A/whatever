package org.springframework.instrument.classloading;

import org.springframework.core.OverridingClassLoader;
import org.springframework.lang.Nullable;

import java.lang.instrument.ClassFileTransformer;

public class SimpleInstrumentableClassLoader extends OverridingClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    private final WeavingTransformer weavingTransformer;

    public SimpleInstrumentableClassLoader(@Nullable ClassLoader parent) {
        super(parent);
        this.weavingTransformer = new WeavingTransformer(parent);
    }

    public void addTransformer(ClassFileTransformer transformer) {
        this.weavingTransformer.addTransformer(transformer);
    }

    @Override
    protected byte[] transformIfNecessary(String name, byte[] bytes) {
        return this.weavingTransformer.transformIfNecessary(name, bytes);
    }

}
