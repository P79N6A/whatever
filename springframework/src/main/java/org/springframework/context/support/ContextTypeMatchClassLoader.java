package org.springframework.context.support;

import org.springframework.core.DecoratingClassLoader;
import org.springframework.core.OverridingClassLoader;
import org.springframework.core.SmartClassLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class ContextTypeMatchClassLoader extends DecoratingClassLoader implements SmartClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    private static Method findLoadedClassMethod;

    static {
        try {
            findLoadedClassMethod = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException("Invalid [java.lang.ClassLoader] class: no 'findLoadedClass' method defined!");
        }
    }

    private final Map<String, byte[]> bytesCache = new ConcurrentHashMap<>(256);

    public ContextTypeMatchClassLoader(@Nullable ClassLoader parent) {
        super(parent);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return new ContextOverridingClassLoader(getParent()).loadClass(name);
    }

    @Override
    public boolean isClassReloadable(Class<?> clazz) {
        return (clazz.getClassLoader() instanceof ContextOverridingClassLoader);
    }

    private class ContextOverridingClassLoader extends OverridingClassLoader {

        public ContextOverridingClassLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        protected boolean isEligibleForOverriding(String className) {
            if (isExcluded(className) || ContextTypeMatchClassLoader.this.isExcluded(className)) {
                return false;
            }
            ReflectionUtils.makeAccessible(findLoadedClassMethod);
            ClassLoader parent = getParent();
            while (parent != null) {
                if (ReflectionUtils.invokeMethod(findLoadedClassMethod, parent, className) != null) {
                    return false;
                }
                parent = parent.getParent();
            }
            return true;
        }

        @Override
        protected Class<?> loadClassForOverriding(String name) throws ClassNotFoundException {
            byte[] bytes = bytesCache.get(name);
            if (bytes == null) {
                bytes = loadBytesForClass(name);
                if (bytes != null) {
                    bytesCache.put(name, bytes);
                } else {
                    return null;
                }
            }
            return defineClass(name, bytes, 0, bytes.length);
        }

    }

}
