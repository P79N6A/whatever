package org.springframework.boot.web.context;

import org.springframework.boot.web.server.WebServer;
import org.springframework.context.ApplicationContext;

public interface WebServerApplicationContext extends ApplicationContext {

    WebServer getWebServer();

    String getServerNamespace();

}
