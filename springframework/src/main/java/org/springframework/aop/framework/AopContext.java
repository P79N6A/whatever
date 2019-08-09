package org.springframework.aop.framework;

import org.springframework.core.NamedThreadLocal;
import org.springframework.lang.Nullable;

public final class AopContext {

    private static final ThreadLocal<Object> currentProxy = new NamedThreadLocal<>("Current AOP proxy");

    private AopContext() {
    }

    public static Object currentProxy() throws IllegalStateException {
        Object proxy = currentProxy.get();
        if (proxy == null) {
            throw new IllegalStateException("Cannot find current proxy: Set 'exposeProxy' property on Advised to 'true' to make it available.");
        }
        return proxy;
    }

    @Nullable
    static Object setCurrentProxy(@Nullable Object proxy) {
        Object old = currentProxy.get();
        if (proxy != null) {
            currentProxy.set(proxy);
        } else {
            currentProxy.remove();
        }
        return old;
    }

}
