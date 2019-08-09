package org.springframework.boot.web.context;

import org.springframework.context.ConfigurableApplicationContext;

public interface ConfigurableWebServerApplicationContext extends ConfigurableApplicationContext, WebServerApplicationContext {

    void setServerNamespace(String serverNamespace);

}
