package org.springframework.web.context;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.Enumeration;

public class ContextCleanupListener implements ServletContextListener {

    private static final Log logger = LogFactory.getLog(ContextCleanupListener.class);

    @Override
    public void contextInitialized(ServletContextEvent event) {
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        cleanupAttributes(event.getServletContext());
    }

    static void cleanupAttributes(ServletContext sc) {
        Enumeration<String> attrNames = sc.getAttributeNames();
        while (attrNames.hasMoreElements()) {
            String attrName = attrNames.nextElement();
            if (attrName.startsWith("org.springframework.")) {
                Object attrValue = sc.getAttribute(attrName);
                if (attrValue instanceof DisposableBean) {
                    try {
                        ((DisposableBean) attrValue).destroy();
                    } catch (Throwable ex) {
                        logger.error("Couldn't invoke destroy method of attribute with name '" + attrName + "'", ex);
                    }
                }
            }
        }
    }

}
