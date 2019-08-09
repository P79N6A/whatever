package org.springframework.web.servlet.view;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class InternalResourceView extends AbstractUrlBasedView {

    private boolean alwaysInclude = false;

    private boolean preventDispatchLoop = false;

    public InternalResourceView() {
    }

    public InternalResourceView(String url) {
        super(url);
    }

    public InternalResourceView(String url, boolean alwaysInclude) {
        super(url);
        this.alwaysInclude = alwaysInclude;
    }

    public void setAlwaysInclude(boolean alwaysInclude) {
        this.alwaysInclude = alwaysInclude;
    }

    public void setPreventDispatchLoop(boolean preventDispatchLoop) {
        this.preventDispatchLoop = preventDispatchLoop;
    }

    @Override
    protected boolean isContextRequired() {
        return false;
    }

    @Override
    protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        // Expose the model object as request attributes.
        exposeModelAsRequestAttributes(model, request);
        // Expose helpers as request attributes, if any.
        exposeHelpers(request);
        // 获取转发路径
        String dispatcherPath = prepareForRendering(request, response);
        // 获取RequestDispatcher
        RequestDispatcher rd = getRequestDispatcher(request, dispatcherPath);
        if (rd == null) {
            throw new ServletException("Could not get RequestDispatcher for [" + getUrl() + "]: Check that the corresponding file exists within your web application archive!");
        }
        // If already included or response already committed, perform include, else forward.
        if (useInclude(request, response)) {
            response.setContentType(getContentType());
            if (logger.isDebugEnabled()) {
                logger.debug("Including [" + getUrl() + "]");
            }
            rd.include(request, response);
        } else {
            // Note: The forwarded resource is supposed to determine the content type itself.
            if (logger.isDebugEnabled()) {
                logger.debug("Forwarding to [" + getUrl() + "]");
            }
            // forward转发
            rd.forward(request, response);
        }
    }

    protected void exposeHelpers(HttpServletRequest request) throws Exception {
    }

    protected String prepareForRendering(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String path = getUrl();
        Assert.state(path != null, "'url' not set");
        if (this.preventDispatchLoop) {
            String uri = request.getRequestURI();
            if (path.startsWith("/") ? uri.equals(path) : uri.equals(StringUtils.applyRelativePath(uri, path))) {
                throw new ServletException("Circular view path [" + path + "]: would dispatch back " + "to the current handler URL [" + uri + "] again. Check your ViewResolver setup! " + "(Hint: This may be the result of an unspecified view, due to default view name generation.)");
            }
        }
        return path;
    }

    @Nullable
    protected RequestDispatcher getRequestDispatcher(HttpServletRequest request, String path) {
        return request.getRequestDispatcher(path);
    }

    protected boolean useInclude(HttpServletRequest request, HttpServletResponse response) {
        return (this.alwaysInclude || WebUtils.isIncludeRequest(request) || response.isCommitted());
    }

}
