package org.springframework.boot.web.server;

@FunctionalInterface
public interface ErrorPageRegistrar {

    void registerErrorPages(ErrorPageRegistry registry);

}
