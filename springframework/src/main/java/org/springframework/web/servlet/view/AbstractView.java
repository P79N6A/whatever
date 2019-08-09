package org.springframework.web.servlet.view;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.ContextExposingHttpServletRequest;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.support.RequestContext;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

public abstract class AbstractView extends WebApplicationObjectSupport implements View, BeanNameAware {

    public static final String DEFAULT_CONTENT_TYPE = "text/html;charset=ISO-8859-1";

    private static final int OUTPUT_BYTE_ARRAY_INITIAL_SIZE = 4096;

    @Nullable
    private String contentType = DEFAULT_CONTENT_TYPE;

    @Nullable
    private String requestContextAttribute;

    private final Map<String, Object> staticAttributes = new LinkedHashMap<>();

    private boolean exposePathVariables = true;

    private boolean exposeContextBeansAsAttributes = false;

    @Nullable
    private Set<String> exposedContextBeanNames;

    @Nullable
    private String beanName;

    public void setContentType(@Nullable String contentType) {
        this.contentType = contentType;
    }

    @Override
    @Nullable
    public String getContentType() {
        return this.contentType;
    }

    public void setRequestContextAttribute(@Nullable String requestContextAttribute) {
        this.requestContextAttribute = requestContextAttribute;
    }

    @Nullable
    public String getRequestContextAttribute() {
        return this.requestContextAttribute;
    }

    public void setAttributesCSV(@Nullable String propString) throws IllegalArgumentException {
        if (propString != null) {
            StringTokenizer st = new StringTokenizer(propString, ",");
            while (st.hasMoreTokens()) {
                String tok = st.nextToken();
                int eqIdx = tok.indexOf('=');
                if (eqIdx == -1) {
                    throw new IllegalArgumentException("Expected '=' in attributes CSV string '" + propString + "'");
                }
                if (eqIdx >= tok.length() - 2) {
                    throw new IllegalArgumentException("At least 2 characters ([]) required in attributes CSV string '" + propString + "'");
                }
                String name = tok.substring(0, eqIdx);
                String value = tok.substring(eqIdx + 1);
                // Delete first and last characters of value: { and }
                value = value.substring(1);
                value = value.substring(0, value.length() - 1);
                addStaticAttribute(name, value);
            }
        }
    }

    public void setAttributes(Properties attributes) {
        CollectionUtils.mergePropertiesIntoMap(attributes, this.staticAttributes);
    }

    public void setAttributesMap(@Nullable Map<String, ?> attributes) {
        if (attributes != null) {
            attributes.forEach(this::addStaticAttribute);
        }
    }

    public Map<String, Object> getAttributesMap() {
        return this.staticAttributes;
    }

    public void addStaticAttribute(String name, Object value) {
        this.staticAttributes.put(name, value);
    }

    public Map<String, Object> getStaticAttributes() {
        return Collections.unmodifiableMap(this.staticAttributes);
    }

    public void setExposePathVariables(boolean exposePathVariables) {
        this.exposePathVariables = exposePathVariables;
    }

    public boolean isExposePathVariables() {
        return this.exposePathVariables;
    }

    public void setExposeContextBeansAsAttributes(boolean exposeContextBeansAsAttributes) {
        this.exposeContextBeansAsAttributes = exposeContextBeansAsAttributes;
    }

    public void setExposedContextBeanNames(String... exposedContextBeanNames) {
        this.exposedContextBeanNames = new HashSet<>(Arrays.asList(exposedContextBeanNames));
    }

    @Override
    public void setBeanName(@Nullable String beanName) {
        this.beanName = beanName;
    }

    @Nullable
    public String getBeanName() {
        return this.beanName;
    }

    /**
     *
     */
    @Override
    public void render(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("View " + formatViewName() + ", model " + (model != null ? model : Collections.emptyMap()) + (this.staticAttributes.isEmpty() ? "" : ", static attributes " + this.staticAttributes));
        }
        Map<String, Object> mergedModel = createMergedOutputModel(model, request, response);
        prepareResponse(request, response);
        // InternalResourceView
        renderMergedOutputModel(mergedModel, getRequestToExpose(request), response);
    }

    protected Map<String, Object> createMergedOutputModel(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) {
        @SuppressWarnings("unchecked") Map<String, Object> pathVars = (this.exposePathVariables ? (Map<String, Object>) request.getAttribute(View.PATH_VARIABLES) : null);
        // Consolidate static and dynamic model attributes.
        int size = this.staticAttributes.size();
        size += (model != null ? model.size() : 0);
        size += (pathVars != null ? pathVars.size() : 0);
        Map<String, Object> mergedModel = new LinkedHashMap<>(size);
        mergedModel.putAll(this.staticAttributes);
        if (pathVars != null) {
            mergedModel.putAll(pathVars);
        }
        if (model != null) {
            mergedModel.putAll(model);
        }
        // Expose RequestContext?
        if (this.requestContextAttribute != null) {
            mergedModel.put(this.requestContextAttribute, createRequestContext(request, response, mergedModel));
        }
        return mergedModel;
    }

    protected RequestContext createRequestContext(HttpServletRequest request, HttpServletResponse response, Map<String, Object> model) {
        return new RequestContext(request, response, getServletContext(), model);
    }

    protected void prepareResponse(HttpServletRequest request, HttpServletResponse response) {
        if (generatesDownloadContent()) {
            response.setHeader("Pragma", "private");
            response.setHeader("Cache-Control", "private, must-revalidate");
        }
    }

    protected boolean generatesDownloadContent() {
        return false;
    }

    protected HttpServletRequest getRequestToExpose(HttpServletRequest originalRequest) {
        if (this.exposeContextBeansAsAttributes || this.exposedContextBeanNames != null) {
            WebApplicationContext wac = getWebApplicationContext();
            Assert.state(wac != null, "No WebApplicationContext");
            return new ContextExposingHttpServletRequest(originalRequest, wac, this.exposedContextBeanNames);
        }
        return originalRequest;
    }

    /**
     *
     */
    protected abstract void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception;

    protected void exposeModelAsRequestAttributes(Map<String, Object> model, HttpServletRequest request) throws Exception {
        model.forEach((name, value) -> {
            if (value != null) {
                request.setAttribute(name, value);
            } else {
                request.removeAttribute(name);
            }
        });
    }

    protected ByteArrayOutputStream createTemporaryOutputStream() {
        return new ByteArrayOutputStream(OUTPUT_BYTE_ARRAY_INITIAL_SIZE);
    }

    protected void writeToResponse(HttpServletResponse response, ByteArrayOutputStream baos) throws IOException {
        // Write content type and also length (determined via byte array).
        response.setContentType(getContentType());
        response.setContentLength(baos.size());
        // Flush byte array to servlet output stream.
        ServletOutputStream out = response.getOutputStream();
        baos.writeTo(out);
        out.flush();
    }

    protected void setResponseContentType(HttpServletRequest request, HttpServletResponse response) {
        MediaType mediaType = (MediaType) request.getAttribute(View.SELECTED_CONTENT_TYPE);
        if (mediaType != null && mediaType.isConcrete()) {
            response.setContentType(mediaType.toString());
        } else {
            response.setContentType(getContentType());
        }
    }

    @Override
    public String toString() {
        return getClass().getName() + ": " + formatViewName();
    }

    protected String formatViewName() {
        return (getBeanName() != null ? "name '" + getBeanName() + "'" : "[" + getClass().getSimpleName() + "]");
    }

}
