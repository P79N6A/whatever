package org.springframework.web.util;

import org.springframework.beans.CachedIntrospectionResults;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.beans.Introspector;

public class IntrospectorCleanupListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        CachedIntrospectionResults.acceptClassLoader(Thread.currentThread().getContextClassLoader());
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        CachedIntrospectionResults.clearClassLoader(Thread.currentThread().getContextClassLoader());
        Introspector.flushCaches();
    }

}
