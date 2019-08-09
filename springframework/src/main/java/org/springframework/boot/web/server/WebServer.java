package org.springframework.boot.web.server;

public interface WebServer {

    void start() throws WebServerException;

    void stop() throws WebServerException;

    int getPort();

}
