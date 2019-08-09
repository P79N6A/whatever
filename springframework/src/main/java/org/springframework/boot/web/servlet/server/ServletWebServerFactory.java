package org.springframework.boot.web.servlet.server;

import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.ServletContextInitializer;

@FunctionalInterface
public interface ServletWebServerFactory {

    WebServer getWebServer(ServletContextInitializer... initializers);

}
