package org.springframework.aop.framework.autoproxy;

import org.springframework.core.NamedThreadLocal;
import org.springframework.lang.Nullable;

public final class ProxyCreationContext {

    private static final ThreadLocal<String> currentProxiedBeanName = new NamedThreadLocal<>("Name of currently proxied bean");

    private ProxyCreationContext() {
    }

    @Nullable
    public static String getCurrentProxiedBeanName() {
        return currentProxiedBeanName.get();
    }

    static void setCurrentProxiedBeanName(@Nullable String beanName) {
        if (beanName != null) {
            currentProxiedBeanName.set(beanName);
        } else {
            currentProxiedBeanName.remove();
        }
    }

}
