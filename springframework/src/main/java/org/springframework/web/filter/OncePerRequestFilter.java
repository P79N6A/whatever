package org.springframework.web.filter;

import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.util.WebUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public abstract class OncePerRequestFilter extends GenericFilterBean {

    public static final String ALREADY_FILTERED_SUFFIX = ".FILTERED";

    @Override
    public final void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            throw new ServletException("OncePerRequestFilter just supports HTTP requests");
        }
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String alreadyFilteredAttributeName = getAlreadyFilteredAttributeName();
        boolean hasAlreadyFilteredAttribute = request.getAttribute(alreadyFilteredAttributeName) != null;
        if (hasAlreadyFilteredAttribute || skipDispatch(httpRequest) || shouldNotFilter(httpRequest)) {
            // Proceed without invoking this filter...
            filterChain.doFilter(request, response);
        } else {
            // Do invoke this filter...
            request.setAttribute(alreadyFilteredAttributeName, Boolean.TRUE);
            try {
                doFilterInternal(httpRequest, httpResponse, filterChain);
            } finally {
                // Remove the "already filtered" request attribute for this request.
                request.removeAttribute(alreadyFilteredAttributeName);
            }
        }
    }

    private boolean skipDispatch(HttpServletRequest request) {
        if (isAsyncDispatch(request) && shouldNotFilterAsyncDispatch()) {
            return true;
        }
        if (request.getAttribute(WebUtils.ERROR_REQUEST_URI_ATTRIBUTE) != null && shouldNotFilterErrorDispatch()) {
            return true;
        }
        return false;
    }

    protected boolean isAsyncDispatch(HttpServletRequest request) {
        return WebAsyncUtils.getAsyncManager(request).hasConcurrentResult();
    }

    protected boolean isAsyncStarted(HttpServletRequest request) {
        return WebAsyncUtils.getAsyncManager(request).isConcurrentHandlingStarted();
    }

    protected String getAlreadyFilteredAttributeName() {
        String name = getFilterName();
        if (name == null) {
            name = getClass().getName();
        }
        return name + ALREADY_FILTERED_SUFFIX;
    }

    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        return false;
    }

    protected boolean shouldNotFilterAsyncDispatch() {
        return true;
    }

    protected boolean shouldNotFilterErrorDispatch() {
        return true;
    }

    protected abstract void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException;

}
