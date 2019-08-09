package org.springframework.web.context.support;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@SuppressWarnings("serial")
public class HttpRequestHandlerServlet extends HttpServlet {

    @Nullable
    private HttpRequestHandler target;

    @Override
    public void init() throws ServletException {
        WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
        this.target = wac.getBean(getServletName(), HttpRequestHandler.class);
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Assert.state(this.target != null, "No HttpRequestHandler available");
        LocaleContextHolder.setLocale(request.getLocale());
        try {
            this.target.handleRequest(request, response);
        } catch (HttpRequestMethodNotSupportedException ex) {
            String[] supportedMethods = ex.getSupportedMethods();
            if (supportedMethods != null) {
                response.setHeader("Allow", StringUtils.arrayToDelimitedString(supportedMethods, ", "));
            }
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, ex.getMessage());
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }

}
