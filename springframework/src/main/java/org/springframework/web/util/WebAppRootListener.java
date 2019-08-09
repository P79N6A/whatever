package org.springframework.web.util;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class WebAppRootListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        WebUtils.setWebAppRootSystemProperty(event.getServletContext());
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        WebUtils.removeWebAppRootSystemProperty(event.getServletContext());
    }

}
