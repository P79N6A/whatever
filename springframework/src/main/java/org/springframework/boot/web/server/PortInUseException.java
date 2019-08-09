package org.springframework.boot.web.server;

public class PortInUseException extends WebServerException {

    private final int port;

    public PortInUseException(int port) {
        super("Port " + port + " is already in use", null);
        this.port = port;
    }

    public int getPort() {
        return this.port;
    }

}
