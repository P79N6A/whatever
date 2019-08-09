package org.springframework.web.filter;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

final class RelativeRedirectResponseWrapper extends HttpServletResponseWrapper {

    private final HttpStatus redirectStatus;

    private RelativeRedirectResponseWrapper(HttpServletResponse response, HttpStatus redirectStatus) {
        super(response);
        Assert.notNull(redirectStatus, "'redirectStatus' is required");
        this.redirectStatus = redirectStatus;
    }

    @Override
    public void sendRedirect(String location) {
        setStatus(this.redirectStatus.value());
        setHeader(HttpHeaders.LOCATION, location);
    }

    public static HttpServletResponse wrapIfNecessary(HttpServletResponse response, HttpStatus redirectStatus) {
        RelativeRedirectResponseWrapper wrapper = WebUtils.getNativeResponse(response, RelativeRedirectResponseWrapper.class);
        return (wrapper != null ? response : new RelativeRedirectResponseWrapper(response, redirectStatus));
    }

}
