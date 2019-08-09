package org.springframework.web.filter;

import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RelativeRedirectFilter extends OncePerRequestFilter {

    private HttpStatus redirectStatus = HttpStatus.SEE_OTHER;

    public void setRedirectStatus(HttpStatus status) {
        Assert.notNull(status, "Property 'redirectStatus' is required");
        Assert.isTrue(status.is3xxRedirection(), "Not a redirect status code");
        this.redirectStatus = status;
    }

    public HttpStatus getRedirectStatus() {
        return this.redirectStatus;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        response = RelativeRedirectResponseWrapper.wrapIfNecessary(response, this.redirectStatus);
        filterChain.doFilter(request, response);
    }

}
