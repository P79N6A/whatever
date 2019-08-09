package org.springframework.boot.web.servlet.context;

import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.server.WebServer;

@SuppressWarnings("serial")
public class ServletWebServerInitializedEvent extends WebServerInitializedEvent {

    private final ServletWebServerApplicationContext applicationContext;

    public ServletWebServerInitializedEvent(WebServer webServer, ServletWebServerApplicationContext applicationContext) {
        super(webServer);
        this.applicationContext = applicationContext;
    }

    @Override
    public ServletWebServerApplicationContext getApplicationContext() {
        return this.applicationContext;
    }

}
