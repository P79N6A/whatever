package org.springframework.cglib.proxy;

import org.springframework.cglib.core.*;
import org.springframework.cglib.reflect.FastClass;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@SuppressWarnings({"rawtypes", "unchecked"})
public class MethodProxy {

    private Signature sig1;

    private Signature sig2;

    private CreateInfo createInfo;

    private final Object initLock = new Object();

    private volatile FastClassInfo fastClassInfo;

    public static MethodProxy create(Class c1, Class c2, String desc, String name1, String name2) {
        MethodProxy proxy = new MethodProxy();
        proxy.sig1 = new Signature(name1, desc);
        proxy.sig2 = new Signature(name2, desc);
        proxy.createInfo = new CreateInfo(c1, c2);
        return proxy;
    }

    private void init() {
        if (fastClassInfo == null) {
            synchronized (initLock) {
                if (fastClassInfo == null) {
                    CreateInfo ci = createInfo;
                    FastClassInfo fci = new FastClassInfo();
                    fci.f1 = helper(ci, ci.c1);
                    fci.f2 = helper(ci, ci.c2);
                    fci.i1 = fci.f1.getIndex(sig1);
                    fci.i2 = fci.f2.getIndex(sig2);
                    fastClassInfo = fci;
                    createInfo = null;
                }
            }
        }
    }

    private static class FastClassInfo {

        FastClass f1;

        FastClass f2;

        int i1;

        int i2;

    }

    private static class CreateInfo {

        Class c1;

        Class c2;

        NamingPolicy namingPolicy;

        GeneratorStrategy strategy;

        boolean attemptLoad;

        public CreateInfo(Class c1, Class c2) {
            this.c1 = c1;
            this.c2 = c2;
            AbstractClassGenerator fromEnhancer = AbstractClassGenerator.getCurrent();
            if (fromEnhancer != null) {
                namingPolicy = fromEnhancer.getNamingPolicy();
                strategy = fromEnhancer.getStrategy();
                attemptLoad = fromEnhancer.getAttemptLoad();
            }
        }

    }

    private static FastClass helper(CreateInfo ci, Class type) {
        FastClass.Generator g = new FastClass.Generator();
        g.setType(type);
        // SPRING PATCH BEGIN
        g.setContextClass(type);
        // SPRING PATCH END
        g.setClassLoader(ci.c2.getClassLoader());
        g.setNamingPolicy(ci.namingPolicy);
        g.setStrategy(ci.strategy);
        g.setAttemptLoad(ci.attemptLoad);
        return g.create();
    }

    private MethodProxy() {
    }

    public Signature getSignature() {
        return sig1;
    }

    public String getSuperName() {
        return sig2.getName();
    }

    public int getSuperIndex() {
        init();
        return fastClassInfo.i2;
    }

    // For testing
    FastClass getFastClass() {
        init();
        return fastClassInfo.f1;
    }

    // For testing
    FastClass getSuperFastClass() {
        init();
        return fastClassInfo.f2;
    }

    public static MethodProxy find(Class type, Signature sig) {
        try {
            Method m = type.getDeclaredMethod(MethodInterceptorGenerator.FIND_PROXY_NAME, MethodInterceptorGenerator.FIND_PROXY_TYPES);
            return (MethodProxy) m.invoke(null, new Object[]{sig});
        } catch (NoSuchMethodException ex) {
            throw new IllegalArgumentException("Class " + type + " does not use a MethodInterceptor");
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new CodeGenerationException(ex);
        }
    }

    public Object invoke(Object obj, Object[] args) throws Throwable {
        try {
            init();
            FastClassInfo fci = fastClassInfo;
            return fci.f1.invoke(fci.i1, obj, args);
        } catch (InvocationTargetException ex) {
            throw ex.getTargetException();
        } catch (IllegalArgumentException ex) {
            if (fastClassInfo.i1 < 0)
                throw new IllegalArgumentException("Protected method: " + sig1);
            throw ex;
        }
    }

    public Object invokeSuper(Object obj, Object[] args) throws Throwable {
        try {
            init();
            FastClassInfo fci = fastClassInfo;
            return fci.f2.invoke(fci.i2, obj, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

}
