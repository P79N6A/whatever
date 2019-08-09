package org.springframework.boot.web.context;

import org.springframework.boot.web.server.WebServer;
import org.springframework.context.ApplicationEvent;

@SuppressWarnings("serial")
public abstract class WebServerInitializedEvent extends ApplicationEvent {

    protected WebServerInitializedEvent(WebServer webServer) {
        super(webServer);
    }

    public WebServer getWebServer() {
        return getSource();
    }

    public abstract WebServerApplicationContext getApplicationContext();

    @Override
    public WebServer getSource() {
        return (WebServer) super.getSource();
    }

}
