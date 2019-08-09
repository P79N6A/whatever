package org.springframework.boot.web.server;

@FunctionalInterface
public interface WebServerFactoryCustomizer<T extends WebServerFactory> {

    void customize(T factory);

}
