package org.springframework.boot;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

class ClearCachesApplicationListener implements ApplicationListener<ContextRefreshedEvent> {

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        ReflectionUtils.clearCache();
        clearClassLoaderCaches(Thread.currentThread().getContextClassLoader());
    }

    private void clearClassLoaderCaches(ClassLoader classLoader) {
        if (classLoader == null) {
            return;
        }
        try {
            Method clearCacheMethod = classLoader.getClass().getDeclaredMethod("clearCache");
            clearCacheMethod.invoke(classLoader);
        } catch (Exception ex) {
            // Ignore
        }
        clearClassLoaderCaches(classLoader.getParent());
    }

}
