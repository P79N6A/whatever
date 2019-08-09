package org.springframework.boot.web.servlet.error;

@FunctionalInterface
public interface ErrorController {

    String getErrorPath();

}
