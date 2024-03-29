package org.springframework.boot.web.servlet.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.ErrorPageRegistry;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.ClassUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.NestedServletException;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ErrorPageFilter implements Filter, ErrorPageRegistry {

    private static final Log logger = LogFactory.getLog(ErrorPageFilter.class);
    // From RequestDispatcher but not referenced to remain compatible with Servlet 2.5

    private static final String ERROR_EXCEPTION = "javax.servlet.error.exception";

    private static final String ERROR_EXCEPTION_TYPE = "javax.servlet.error.exception_type";

    private static final String ERROR_MESSAGE = "javax.servlet.error.message";

    public static final String ERROR_REQUEST_URI = "javax.servlet.error.request_uri";

    private static final String ERROR_STATUS_CODE = "javax.servlet.error.status_code";

    private static final Set<Class<?>> CLIENT_ABORT_EXCEPTIONS;

    static {
        Set<Class<?>> clientAbortExceptions = new HashSet<>();
        addClassIfPresent(clientAbortExceptions, "org.apache.catalina.connector.ClientAbortException");
        CLIENT_ABORT_EXCEPTIONS = Collections.unmodifiableSet(clientAbortExceptions);
    }

    private String global;

    private final Map<Integer, String> statuses = new HashMap<>();

    private final Map<Class<?>, String> exceptions = new HashMap<>();

    private final OncePerRequestFilter delegate = new OncePerRequestFilter() {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
            ErrorPageFilter.this.doFilter(request, response, chain);
        }

        @Override
        protected boolean shouldNotFilterAsyncDispatch() {
            return false;
        }

    };

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.delegate.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        this.delegate.doFilter(request, response, chain);
    }

    private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        ErrorWrapperResponse wrapped = new ErrorWrapperResponse(response);
        try {
            chain.doFilter(request, wrapped);
            if (wrapped.hasErrorToSend()) {
                handleErrorStatus(request, response, wrapped.getStatus(), wrapped.getMessage());
                response.flushBuffer();
            } else if (!request.isAsyncStarted() && !response.isCommitted()) {
                response.flushBuffer();
            }
        } catch (Throwable ex) {
            Throwable exceptionToHandle = ex;
            if (ex instanceof NestedServletException) {
                exceptionToHandle = ((NestedServletException) ex).getRootCause();
            }
            handleException(request, response, wrapped, exceptionToHandle);
            response.flushBuffer();
        }
    }

    private void handleErrorStatus(HttpServletRequest request, HttpServletResponse response, int status, String message) throws ServletException, IOException {
        if (response.isCommitted()) {
            handleCommittedResponse(request, null);
            return;
        }
        String errorPath = getErrorPath(this.statuses, status);
        if (errorPath == null) {
            response.sendError(status, message);
            return;
        }
        response.setStatus(status);
        setErrorAttributes(request, status, message);
        request.getRequestDispatcher(errorPath).forward(request, response);
    }

    private void handleException(HttpServletRequest request, HttpServletResponse response, ErrorWrapperResponse wrapped, Throwable ex) throws IOException, ServletException {
        Class<?> type = ex.getClass();
        String errorPath = getErrorPath(type);
        if (errorPath == null) {
            rethrow(ex);
            return;
        }
        if (response.isCommitted()) {
            handleCommittedResponse(request, ex);
            return;
        }
        forwardToErrorPage(errorPath, request, wrapped, ex);
    }

    private void forwardToErrorPage(String path, HttpServletRequest request, HttpServletResponse response, Throwable ex) throws ServletException, IOException {
        if (logger.isErrorEnabled()) {
            String message = "Forwarding to error page from request " + getDescription(request) + " due to exception [" + ex.getMessage() + "]";
            logger.error(message, ex);
        }
        setErrorAttributes(request, 500, ex.getMessage());
        request.setAttribute(ERROR_EXCEPTION, ex);
        request.setAttribute(ERROR_EXCEPTION_TYPE, ex.getClass());
        response.reset();
        response.setStatus(500);
        request.getRequestDispatcher(path).forward(request, response);
        request.removeAttribute(ERROR_EXCEPTION);
        request.removeAttribute(ERROR_EXCEPTION_TYPE);
    }

    protected String getDescription(HttpServletRequest request) {
        String pathInfo = (request.getPathInfo() != null) ? request.getPathInfo() : "";
        return "[" + request.getServletPath() + pathInfo + "]";
    }

    private void handleCommittedResponse(HttpServletRequest request, Throwable ex) {
        if (isClientAbortException(ex)) {
            return;
        }
        String message = "Cannot forward to error page for request " + getDescription(request) + " as the response has already been" + " committed. As a result, the response may have the wrong status" + " code. If your application is running on WebSphere Application" + " Server you may be able to resolve this problem by setting" + " com.ibm.ws.webcontainer.invokeFlushAfterService to false";
        if (ex == null) {
            logger.error(message);
        } else {
            // User might see the error page without all the data here but throwing the
            // exception isn't going to help anyone (we'll log it to be on the safe side)
            logger.error(message, ex);
        }
    }

    private boolean isClientAbortException(Throwable ex) {
        if (ex == null) {
            return false;
        }
        for (Class<?> candidate : CLIENT_ABORT_EXCEPTIONS) {
            if (candidate.isInstance(ex)) {
                return true;
            }
        }
        return isClientAbortException(ex.getCause());
    }

    private String getErrorPath(Map<Integer, String> map, Integer status) {
        if (map.containsKey(status)) {
            return map.get(status);
        }
        return this.global;
    }

    private String getErrorPath(Class<?> type) {
        while (type != Object.class) {
            String path = this.exceptions.get(type);
            if (path != null) {
                return path;
            }
            type = type.getSuperclass();
        }
        return this.global;
    }

    private void setErrorAttributes(HttpServletRequest request, int status, String message) {
        request.setAttribute(ERROR_STATUS_CODE, status);
        request.setAttribute(ERROR_MESSAGE, message);
        request.setAttribute(ERROR_REQUEST_URI, request.getRequestURI());
    }

    private void rethrow(Throwable ex) throws IOException, ServletException {
        if (ex instanceof RuntimeException) {
            throw (RuntimeException) ex;
        }
        if (ex instanceof Error) {
            throw (Error) ex;
        }
        if (ex instanceof IOException) {
            throw (IOException) ex;
        }
        if (ex instanceof ServletException) {
            throw (ServletException) ex;
        }
        throw new IllegalStateException(ex);
    }

    @Override
    public void addErrorPages(ErrorPage... errorPages) {
        for (ErrorPage errorPage : errorPages) {
            if (errorPage.isGlobal()) {
                this.global = errorPage.getPath();
            } else if (errorPage.getStatus() != null) {
                this.statuses.put(errorPage.getStatus().value(), errorPage.getPath());
            } else {
                this.exceptions.put(errorPage.getException(), errorPage.getPath());
            }
        }
    }

    @Override
    public void destroy() {
    }

    private static void addClassIfPresent(Collection<Class<?>> collection, String className) {
        try {
            collection.add(ClassUtils.forName(className, null));
        } catch (Throwable ex) {
        }
    }

    private static class ErrorWrapperResponse extends HttpServletResponseWrapper {

        private int status;

        private String message;

        private boolean hasErrorToSend = false;

        ErrorWrapperResponse(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void sendError(int status) throws IOException {
            sendError(status, null);
        }

        @Override
        public void sendError(int status, String message) throws IOException {
            this.status = status;
            this.message = message;
            this.hasErrorToSend = true;
            // Do not call super because the container may prevent us from handling the
            // error ourselves
        }

        @Override
        public int getStatus() {
            if (this.hasErrorToSend) {
                return this.status;
            }
            // If there was no error we need to trust the wrapped response
            return super.getStatus();
        }

        @Override
        public void flushBuffer() throws IOException {
            sendErrorIfNecessary();
            super.flushBuffer();
        }

        private void sendErrorIfNecessary() throws IOException {
            if (this.hasErrorToSend && !isCommitted()) {
                ((HttpServletResponse) getResponse()).sendError(this.status, this.message);
            }
        }

        public String getMessage() {
            return this.message;
        }

        public boolean hasErrorToSend() {
            return this.hasErrorToSend;
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            sendErrorIfNecessary();
            return super.getWriter();

        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            sendErrorIfNecessary();
            return super.getOutputStream();
        }

    }

}
