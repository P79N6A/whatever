package org.springframework.core;

import org.springframework.lang.Nullable;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStream;

public class OverridingClassLoader extends DecoratingClassLoader {

    public static final String[] DEFAULT_EXCLUDED_PACKAGES = new String[]{"java.", "javax.", "sun.", "oracle.", "javassist.", "org.aspectj.", "net.sf.cglib."};

    private static final String CLASS_FILE_SUFFIX = ".class";

    static {
        ClassLoader.registerAsParallelCapable();
    }

    @Nullable
    private final ClassLoader overrideDelegate;

    public OverridingClassLoader(@Nullable ClassLoader parent) {
        this(parent, null);
    }

    public OverridingClassLoader(@Nullable ClassLoader parent, @Nullable ClassLoader overrideDelegate) {
        super(parent);
        this.overrideDelegate = overrideDelegate;
        for (String packageName : DEFAULT_EXCLUDED_PACKAGES) {
            excludePackage(packageName);
        }
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (this.overrideDelegate != null && isEligibleForOverriding(name)) {
            return this.overrideDelegate.loadClass(name);
        }
        return super.loadClass(name);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (isEligibleForOverriding(name)) {
            Class<?> result = loadClassForOverriding(name);
            if (result != null) {
                if (resolve) {
                    resolveClass(result);
                }
                return result;
            }
        }
        return super.loadClass(name, resolve);
    }

    protected boolean isEligibleForOverriding(String className) {
        return !isExcluded(className);
    }

    @Nullable
    protected Class<?> loadClassForOverriding(String name) throws ClassNotFoundException {
        Class<?> result = findLoadedClass(name);
        if (result == null) {
            byte[] bytes = loadBytesForClass(name);
            if (bytes != null) {
                result = defineClass(name, bytes, 0, bytes.length);
            }
        }
        return result;
    }

    @Nullable
    protected byte[] loadBytesForClass(String name) throws ClassNotFoundException {
        InputStream is = openStreamForClass(name);
        if (is == null) {
            return null;
        }
        try {
            // Load the raw bytes.
            byte[] bytes = FileCopyUtils.copyToByteArray(is);
            // Transform if necessary and use the potentially transformed bytes.
            return transformIfNecessary(name, bytes);
        } catch (IOException ex) {
            throw new ClassNotFoundException("Cannot load resource for class [" + name + "]", ex);
        }
    }

    @Nullable
    protected InputStream openStreamForClass(String name) {
        String internalName = name.replace('.', '/') + CLASS_FILE_SUFFIX;
        return getParent().getResourceAsStream(internalName);
    }

    protected byte[] transformIfNecessary(String name, byte[] bytes) {
        return bytes;
    }

}
