package org.springframework.web.servlet.view;

import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.SmartView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.support.RequestDataValueProcessor;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RedirectView extends AbstractUrlBasedView implements SmartView {

    private static final Pattern URI_TEMPLATE_VARIABLE_PATTERN = Pattern.compile("\\{([^/]+?)\\}");

    private boolean contextRelative = false;

    private boolean http10Compatible = true;

    private boolean exposeModelAttributes = true;

    @Nullable
    private String encodingScheme;

    @Nullable
    private HttpStatus statusCode;

    private boolean expandUriTemplateVariables = true;

    private boolean propagateQueryParams = false;

    @Nullable
    private String[] hosts;

    public RedirectView() {
        setExposePathVariables(false);
    }

    public RedirectView(String url) {
        super(url);
        setExposePathVariables(false);
    }

    public RedirectView(String url, boolean contextRelative) {
        super(url);
        this.contextRelative = contextRelative;
        setExposePathVariables(false);
    }

    public RedirectView(String url, boolean contextRelative, boolean http10Compatible) {
        super(url);
        this.contextRelative = contextRelative;
        this.http10Compatible = http10Compatible;
        setExposePathVariables(false);
    }

    public RedirectView(String url, boolean contextRelative, boolean http10Compatible, boolean exposeModelAttributes) {
        super(url);
        this.contextRelative = contextRelative;
        this.http10Compatible = http10Compatible;
        this.exposeModelAttributes = exposeModelAttributes;
        setExposePathVariables(false);
    }

    public void setContextRelative(boolean contextRelative) {
        this.contextRelative = contextRelative;
    }

    public void setHttp10Compatible(boolean http10Compatible) {
        this.http10Compatible = http10Compatible;
    }

    public void setExposeModelAttributes(final boolean exposeModelAttributes) {
        this.exposeModelAttributes = exposeModelAttributes;
    }

    public void setEncodingScheme(String encodingScheme) {
        this.encodingScheme = encodingScheme;
    }

    public void setStatusCode(HttpStatus statusCode) {
        this.statusCode = statusCode;
    }

    public void setExpandUriTemplateVariables(boolean expandUriTemplateVariables) {
        this.expandUriTemplateVariables = expandUriTemplateVariables;
    }

    public void setPropagateQueryParams(boolean propagateQueryParams) {
        this.propagateQueryParams = propagateQueryParams;
    }

    public boolean isPropagateQueryProperties() {
        return this.propagateQueryParams;
    }

    public void setHosts(@Nullable String... hosts) {
        this.hosts = hosts;
    }

    @Nullable
    public String[] getHosts() {
        return this.hosts;
    }

    @Override
    public boolean isRedirectView() {
        return true;
    }

    @Override
    protected boolean isContextRequired() {
        return false;
    }

    @Override
    protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String targetUrl = createTargetUrl(model, request);
        targetUrl = updateTargetUrl(targetUrl, model, request, response);
        // Save flash attributes
        RequestContextUtils.saveOutputFlashMap(targetUrl, request, response);
        // Redirect
        sendRedirect(request, response, targetUrl, this.http10Compatible);
    }

    protected final String createTargetUrl(Map<String, Object> model, HttpServletRequest request) throws UnsupportedEncodingException {
        // Prepare target URL.
        StringBuilder targetUrl = new StringBuilder();
        String url = getUrl();
        Assert.state(url != null, "'url' not set");
        if (this.contextRelative && getUrl().startsWith("/")) {
            // Do not apply context path to relative URLs.
            targetUrl.append(getContextPath(request));
        }
        targetUrl.append(getUrl());
        String enc = this.encodingScheme;
        if (enc == null) {
            enc = request.getCharacterEncoding();
        }
        if (enc == null) {
            enc = WebUtils.DEFAULT_CHARACTER_ENCODING;
        }
        if (this.expandUriTemplateVariables && StringUtils.hasText(targetUrl)) {
            Map<String, String> variables = getCurrentRequestUriVariables(request);
            targetUrl = replaceUriTemplateVariables(targetUrl.toString(), model, variables, enc);
        }
        if (isPropagateQueryProperties()) {
            appendCurrentQueryParams(targetUrl, request);
        }
        if (this.exposeModelAttributes) {
            appendQueryProperties(targetUrl, model, enc);
        }
        return targetUrl.toString();
    }

    private String getContextPath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        while (contextPath.startsWith("//")) {
            contextPath = contextPath.substring(1);
        }
        return contextPath;
    }

    protected StringBuilder replaceUriTemplateVariables(String targetUrl, Map<String, Object> model, Map<String, String> currentUriVariables, String encodingScheme) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        Matcher matcher = URI_TEMPLATE_VARIABLE_PATTERN.matcher(targetUrl);
        int endLastMatch = 0;
        while (matcher.find()) {
            String name = matcher.group(1);
            Object value = (model.containsKey(name) ? model.remove(name) : currentUriVariables.get(name));
            if (value == null) {
                throw new IllegalArgumentException("Model has no value for key '" + name + "'");
            }
            result.append(targetUrl.substring(endLastMatch, matcher.start()));
            result.append(UriUtils.encodePathSegment(value.toString(), encodingScheme));
            endLastMatch = matcher.end();
        }
        result.append(targetUrl.substring(endLastMatch, targetUrl.length()));
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getCurrentRequestUriVariables(HttpServletRequest request) {
        String name = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
        Map<String, String> uriVars = (Map<String, String>) request.getAttribute(name);
        return (uriVars != null) ? uriVars : Collections.<String, String>emptyMap();
    }

    protected void appendCurrentQueryParams(StringBuilder targetUrl, HttpServletRequest request) {
        String query = request.getQueryString();
        if (StringUtils.hasText(query)) {
            // Extract anchor fragment, if any.
            String fragment = null;
            int anchorIndex = targetUrl.indexOf("#");
            if (anchorIndex > -1) {
                fragment = targetUrl.substring(anchorIndex);
                targetUrl.delete(anchorIndex, targetUrl.length());
            }
            if (targetUrl.toString().indexOf('?') < 0) {
                targetUrl.append('?').append(query);
            } else {
                targetUrl.append('&').append(query);
            }
            // Append anchor fragment, if any, to end of URL.
            if (fragment != null) {
                targetUrl.append(fragment);
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void appendQueryProperties(StringBuilder targetUrl, Map<String, Object> model, String encodingScheme) throws UnsupportedEncodingException {
        // Extract anchor fragment, if any.
        String fragment = null;
        int anchorIndex = targetUrl.indexOf("#");
        if (anchorIndex > -1) {
            fragment = targetUrl.substring(anchorIndex);
            targetUrl.delete(anchorIndex, targetUrl.length());
        }
        // If there aren't already some parameters, we need a "?".
        boolean first = (targetUrl.toString().indexOf('?') < 0);
        for (Map.Entry<String, Object> entry : queryProperties(model).entrySet()) {
            Object rawValue = entry.getValue();
            Iterator<Object> valueIter;
            if (rawValue != null && rawValue.getClass().isArray()) {
                valueIter = Arrays.asList(ObjectUtils.toObjectArray(rawValue)).iterator();
            } else if (rawValue instanceof Collection) {
                valueIter = ((Collection<Object>) rawValue).iterator();
            } else {
                valueIter = Collections.singleton(rawValue).iterator();
            }
            while (valueIter.hasNext()) {
                Object value = valueIter.next();
                if (first) {
                    targetUrl.append('?');
                    first = false;
                } else {
                    targetUrl.append('&');
                }
                String encodedKey = urlEncode(entry.getKey(), encodingScheme);
                String encodedValue = (value != null ? urlEncode(value.toString(), encodingScheme) : "");
                targetUrl.append(encodedKey).append('=').append(encodedValue);
            }
        }
        // Append anchor fragment, if any, to end of URL.
        if (fragment != null) {
            targetUrl.append(fragment);
        }
    }

    protected Map<String, Object> queryProperties(Map<String, Object> model) {
        Map<String, Object> result = new LinkedHashMap<>();
        model.forEach((name, value) -> {
            if (isEligibleProperty(name, value)) {
                result.put(name, value);
            }
        });
        return result;
    }

    protected boolean isEligibleProperty(String key, @Nullable Object value) {
        if (value == null) {
            return false;
        }
        if (isEligibleValue(value)) {
            return true;
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            if (length == 0) {
                return false;
            }
            for (int i = 0; i < length; i++) {
                Object element = Array.get(value, i);
                if (!isEligibleValue(element)) {
                    return false;
                }
            }
            return true;
        }
        if (value instanceof Collection) {
            Collection<?> coll = (Collection<?>) value;
            if (coll.isEmpty()) {
                return false;
            }
            for (Object element : coll) {
                if (!isEligibleValue(element)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    protected boolean isEligibleValue(@Nullable Object value) {
        return (value != null && BeanUtils.isSimpleValueType(value.getClass()));
    }

    protected String urlEncode(String input, String encodingScheme) throws UnsupportedEncodingException {
        return URLEncoder.encode(input, encodingScheme);
    }

    protected String updateTargetUrl(String targetUrl, Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) {
        WebApplicationContext wac = getWebApplicationContext();
        if (wac == null) {
            wac = RequestContextUtils.findWebApplicationContext(request, getServletContext());
        }
        if (wac != null && wac.containsBean(RequestContextUtils.REQUEST_DATA_VALUE_PROCESSOR_BEAN_NAME)) {
            RequestDataValueProcessor processor = wac.getBean(RequestContextUtils.REQUEST_DATA_VALUE_PROCESSOR_BEAN_NAME, RequestDataValueProcessor.class);
            return processor.processUrl(request, targetUrl);
        }
        return targetUrl;
    }

    protected void sendRedirect(HttpServletRequest request, HttpServletResponse response, String targetUrl, boolean http10Compatible) throws IOException {
        String encodedURL = (isRemoteHost(targetUrl) ? targetUrl : response.encodeRedirectURL(targetUrl));
        if (http10Compatible) {
            HttpStatus attributeStatusCode = (HttpStatus) request.getAttribute(View.RESPONSE_STATUS_ATTRIBUTE);
            if (this.statusCode != null) {
                response.setStatus(this.statusCode.value());
                response.setHeader("Location", encodedURL);
            } else if (attributeStatusCode != null) {
                response.setStatus(attributeStatusCode.value());
                response.setHeader("Location", encodedURL);
            } else {
                // Send status code 302 by default.
                response.sendRedirect(encodedURL);
            }
        } else {
            HttpStatus statusCode = getHttp11StatusCode(request, response, targetUrl);
            response.setStatus(statusCode.value());
            response.setHeader("Location", encodedURL);
        }
    }

    protected boolean isRemoteHost(String targetUrl) {
        if (ObjectUtils.isEmpty(getHosts())) {
            return false;
        }
        String targetHost = UriComponentsBuilder.fromUriString(targetUrl).build().getHost();
        if (!StringUtils.hasLength(targetHost)) {
            return false;
        }
        for (String host : getHosts()) {
            if (targetHost.equals(host)) {
                return false;
            }
        }
        return true;
    }

    protected HttpStatus getHttp11StatusCode(HttpServletRequest request, HttpServletResponse response, String targetUrl) {
        if (this.statusCode != null) {
            return this.statusCode;
        }
        HttpStatus attributeStatusCode = (HttpStatus) request.getAttribute(View.RESPONSE_STATUS_ATTRIBUTE);
        if (attributeStatusCode != null) {
            return attributeStatusCode;
        }
        return HttpStatus.SEE_OTHER;
    }

}
