package org.springframework.web.filter;

import javax.servlet.http.HttpServletRequest;

public class ServletContextRequestLoggingFilter extends AbstractRequestLoggingFilter {

    @Override
    protected void beforeRequest(HttpServletRequest request, String message) {
        getServletContext().log(message);
    }

    @Override
    protected void afterRequest(HttpServletRequest request, String message) {
        getServletContext().log(message);
    }

}
