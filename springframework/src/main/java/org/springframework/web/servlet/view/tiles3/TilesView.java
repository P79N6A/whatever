package org.springframework.web.servlet.view.tiles3;

import org.apache.tiles.TilesContainer;
import org.apache.tiles.access.TilesAccess;
import org.apache.tiles.renderer.DefinitionRenderer;
import org.apache.tiles.request.AbstractRequest;
import org.apache.tiles.request.ApplicationContext;
import org.apache.tiles.request.Request;
import org.apache.tiles.request.render.Renderer;
import org.apache.tiles.request.servlet.ServletRequest;
import org.apache.tiles.request.servlet.ServletUtil;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.JstlUtils;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.view.AbstractUrlBasedView;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;
import java.util.Map;

public class TilesView extends AbstractUrlBasedView {

    @Nullable
    private Renderer renderer;

    private boolean exposeJstlAttributes = true;

    private boolean alwaysInclude = false;

    @Nullable
    private ApplicationContext applicationContext;

    public void setRenderer(Renderer renderer) {
        this.renderer = renderer;
    }

    protected void setExposeJstlAttributes(boolean exposeJstlAttributes) {
        this.exposeJstlAttributes = exposeJstlAttributes;
    }

    public void setAlwaysInclude(boolean alwaysInclude) {
        this.alwaysInclude = alwaysInclude;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        ServletContext servletContext = getServletContext();
        Assert.state(servletContext != null, "No ServletContext");
        this.applicationContext = ServletUtil.getApplicationContext(servletContext);
        if (this.renderer == null) {
            TilesContainer container = TilesAccess.getContainer(this.applicationContext);
            this.renderer = new DefinitionRenderer(container);
        }
    }

    @Override
    public boolean checkResource(final Locale locale) throws Exception {
        Assert.state(this.renderer != null, "No Renderer set");
        HttpServletRequest servletRequest = null;
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes) {
            servletRequest = ((ServletRequestAttributes) requestAttributes).getRequest();
        }
        Request request = new ServletRequest(this.applicationContext, servletRequest, null) {
            @Override
            public Locale getRequestLocale() {
                return locale;
            }
        };
        return this.renderer.isRenderable(getUrl(), request);
    }

    @Override
    protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Assert.state(this.renderer != null, "No Renderer set");
        exposeModelAsRequestAttributes(model, request);
        if (this.exposeJstlAttributes) {
            JstlUtils.exposeLocalizationContext(new RequestContext(request, getServletContext()));
        }
        if (this.alwaysInclude) {
            request.setAttribute(AbstractRequest.FORCE_INCLUDE_ATTRIBUTE_NAME, true);
        }
        Request tilesRequest = createTilesRequest(request, response);
        this.renderer.render(getUrl(), tilesRequest);
    }

    protected Request createTilesRequest(final HttpServletRequest request, HttpServletResponse response) {
        return new ServletRequest(this.applicationContext, request, response) {
            @Override
            public Locale getRequestLocale() {
                return RequestContextUtils.getLocale(request);
            }
        };
    }

}
