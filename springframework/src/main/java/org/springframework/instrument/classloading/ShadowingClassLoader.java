package org.springframework.instrument.classloading;

import org.springframework.core.DecoratingClassLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.net.URL;
import java.util.*;

public class ShadowingClassLoader extends DecoratingClassLoader {

    public static final String[] DEFAULT_EXCLUDED_PACKAGES = new String[]{"java.", "javax.", "jdk.", "sun.", "oracle.", "com.sun.", "com.ibm.", "COM.ibm.", "org.w3c.", "org.xml.", "org.dom4j.", "org.eclipse", "org.aspectj.", "net.sf.cglib", "org.springframework.cglib", "org.apache.xerces.", "org.apache.commons.logging."};

    private final ClassLoader enclosingClassLoader;

    private final List<ClassFileTransformer> classFileTransformers = new LinkedList<>();

    private final Map<String, Class<?>> classCache = new HashMap<>();

    public ShadowingClassLoader(ClassLoader enclosingClassLoader) {
        this(enclosingClassLoader, true);
    }

    public ShadowingClassLoader(ClassLoader enclosingClassLoader, boolean defaultExcludes) {
        Assert.notNull(enclosingClassLoader, "Enclosing ClassLoader must not be null");
        this.enclosingClassLoader = enclosingClassLoader;
        if (defaultExcludes) {
            for (String excludedPackage : DEFAULT_EXCLUDED_PACKAGES) {
                excludePackage(excludedPackage);
            }
        }
    }

    public void addTransformer(ClassFileTransformer transformer) {
        Assert.notNull(transformer, "Transformer must not be null");
        this.classFileTransformers.add(transformer);
    }

    public void copyTransformers(ShadowingClassLoader other) {
        Assert.notNull(other, "Other ClassLoader must not be null");
        this.classFileTransformers.addAll(other.classFileTransformers);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (shouldShadow(name)) {
            Class<?> cls = this.classCache.get(name);
            if (cls != null) {
                return cls;
            }
            return doLoadClass(name);
        } else {
            return this.enclosingClassLoader.loadClass(name);
        }
    }

    private boolean shouldShadow(String className) {
        return (!className.equals(getClass().getName()) && !className.endsWith("ShadowingClassLoader") && isEligibleForShadowing(className));
    }

    protected boolean isEligibleForShadowing(String className) {
        return !isExcluded(className);
    }

    private Class<?> doLoadClass(String name) throws ClassNotFoundException {
        String internalName = StringUtils.replace(name, ".", "/") + ".class";
        InputStream is = this.enclosingClassLoader.getResourceAsStream(internalName);
        if (is == null) {
            throw new ClassNotFoundException(name);
        }
        try {
            byte[] bytes = FileCopyUtils.copyToByteArray(is);
            bytes = applyTransformers(name, bytes);
            Class<?> cls = defineClass(name, bytes, 0, bytes.length);
            // Additional check for defining the package, if not defined yet.
            if (cls.getPackage() == null) {
                int packageSeparator = name.lastIndexOf('.');
                if (packageSeparator != -1) {
                    String packageName = name.substring(0, packageSeparator);
                    definePackage(packageName, null, null, null, null, null, null, null);
                }
            }
            this.classCache.put(name, cls);
            return cls;
        } catch (IOException ex) {
            throw new ClassNotFoundException("Cannot load resource for class [" + name + "]", ex);
        }
    }

    private byte[] applyTransformers(String name, byte[] bytes) {
        String internalName = StringUtils.replace(name, ".", "/");
        try {
            for (ClassFileTransformer transformer : this.classFileTransformers) {
                byte[] transformed = transformer.transform(this, internalName, null, null, bytes);
                bytes = (transformed != null ? transformed : bytes);
            }
            return bytes;
        } catch (IllegalClassFormatException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public URL getResource(String name) {
        return this.enclosingClassLoader.getResource(name);
    }

    @Override
    @Nullable
    public InputStream getResourceAsStream(String name) {
        return this.enclosingClassLoader.getResourceAsStream(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return this.enclosingClassLoader.getResources(name);
    }

}
