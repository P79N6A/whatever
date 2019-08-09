package org.springframework.boot.web.server;

@FunctionalInterface
public interface ErrorPageRegistry {

    void addErrorPages(ErrorPage... errorPages);

}
