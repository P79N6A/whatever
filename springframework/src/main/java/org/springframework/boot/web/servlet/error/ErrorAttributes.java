package org.springframework.boot.web.servlet.error;

import org.springframework.web.context.request.WebRequest;

import java.util.Map;

public interface ErrorAttributes {

    Map<String, Object> getErrorAttributes(WebRequest webRequest, boolean includeStackTrace);

    Throwable getError(WebRequest webRequest);

}
