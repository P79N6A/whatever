package org.springframework.web.servlet.view;

import org.springframework.web.servlet.support.RequestContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class AbstractTemplateView extends AbstractUrlBasedView {

    public static final String SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE = "springMacroRequestContext";

    private boolean exposeRequestAttributes = false;

    private boolean allowRequestOverride = false;

    private boolean exposeSessionAttributes = false;

    private boolean allowSessionOverride = false;

    private boolean exposeSpringMacroHelpers = true;

    public void setExposeRequestAttributes(boolean exposeRequestAttributes) {
        this.exposeRequestAttributes = exposeRequestAttributes;
    }

    public void setAllowRequestOverride(boolean allowRequestOverride) {
        this.allowRequestOverride = allowRequestOverride;
    }

    public void setExposeSessionAttributes(boolean exposeSessionAttributes) {
        this.exposeSessionAttributes = exposeSessionAttributes;
    }

    public void setAllowSessionOverride(boolean allowSessionOverride) {
        this.allowSessionOverride = allowSessionOverride;
    }

    public void setExposeSpringMacroHelpers(boolean exposeSpringMacroHelpers) {
        this.exposeSpringMacroHelpers = exposeSpringMacroHelpers;
    }

    @Override
    protected final void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (this.exposeRequestAttributes) {
            Map<String, Object> exposed = null;
            for (Enumeration<String> en = request.getAttributeNames(); en.hasMoreElements(); ) {
                String attribute = en.nextElement();
                if (model.containsKey(attribute) && !this.allowRequestOverride) {
                    throw new ServletException("Cannot expose request attribute '" + attribute + "' because of an existing model object of the same name");
                }
                Object attributeValue = request.getAttribute(attribute);
                if (logger.isDebugEnabled()) {
                    exposed = exposed != null ? exposed : new LinkedHashMap<>();
                    exposed.put(attribute, attributeValue);
                }
                model.put(attribute, attributeValue);
            }
            if (logger.isTraceEnabled() && exposed != null) {
                logger.trace("Exposed request attributes to model: " + exposed);
            }
        }
        if (this.exposeSessionAttributes) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                Map<String, Object> exposed = null;
                for (Enumeration<String> en = session.getAttributeNames(); en.hasMoreElements(); ) {
                    String attribute = en.nextElement();
                    if (model.containsKey(attribute) && !this.allowSessionOverride) {
                        throw new ServletException("Cannot expose session attribute '" + attribute + "' because of an existing model object of the same name");
                    }
                    Object attributeValue = session.getAttribute(attribute);
                    if (logger.isDebugEnabled()) {
                        exposed = exposed != null ? exposed : new LinkedHashMap<>();
                        exposed.put(attribute, attributeValue);
                    }
                    model.put(attribute, attributeValue);
                }
                if (logger.isTraceEnabled() && exposed != null) {
                    logger.trace("Exposed session attributes to model: " + exposed);
                }
            }
        }
        if (this.exposeSpringMacroHelpers) {
            if (model.containsKey(SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE)) {
                throw new ServletException("Cannot expose bind macro helper '" + SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE + "' because of an existing model object of the same name");
            }
            // Expose RequestContext instance for Spring macros.
            model.put(SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE, new RequestContext(request, response, getServletContext(), model));
        }
        applyContentType(response);
        if (logger.isDebugEnabled()) {
            logger.debug("Rendering [" + getUrl() + "]");
        }
        renderMergedTemplateModel(model, request, response);
    }

    protected void applyContentType(HttpServletResponse response) {
        if (response.getContentType() == null) {
            response.setContentType(getContentType());
        }
    }

    protected abstract void renderMergedTemplateModel(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception;

}
