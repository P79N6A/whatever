package org.springframework.web.servlet.resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

public class ResourceUrlEncodingFilter extends GenericFilterBean {

    private static final Log logger = LogFactory.getLog(ResourceUrlEncodingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            throw new ServletException("ResourceUrlEncodingFilter only supports HTTP requests");
        }
        ResourceUrlEncodingRequestWrapper wrappedRequest = new ResourceUrlEncodingRequestWrapper((HttpServletRequest) request);
        ResourceUrlEncodingResponseWrapper wrappedResponse = new ResourceUrlEncodingResponseWrapper(wrappedRequest, (HttpServletResponse) response);
        filterChain.doFilter(wrappedRequest, wrappedResponse);
    }

    private static class ResourceUrlEncodingRequestWrapper extends HttpServletRequestWrapper {

        @Nullable
        private ResourceUrlProvider resourceUrlProvider;

        @Nullable
        private Integer indexLookupPath;

        private String prefixLookupPath = "";

        ResourceUrlEncodingRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public void setAttribute(String name, Object value) {
            super.setAttribute(name, value);
            if (ResourceUrlProviderExposingInterceptor.RESOURCE_URL_PROVIDER_ATTR.equals(name)) {
                if (value instanceof ResourceUrlProvider) {
                    initLookupPath((ResourceUrlProvider) value);
                }
            }
        }

        private void initLookupPath(ResourceUrlProvider urlProvider) {
            this.resourceUrlProvider = urlProvider;
            if (this.indexLookupPath == null) {
                UrlPathHelper pathHelper = this.resourceUrlProvider.getUrlPathHelper();
                String requestUri = pathHelper.getRequestUri(this);
                String lookupPath = pathHelper.getLookupPathForRequest(this);
                this.indexLookupPath = requestUri.lastIndexOf(lookupPath);
                if (this.indexLookupPath == -1) {
                    throw new IllegalStateException("Failed to find lookupPath '" + lookupPath + "' within requestUri '" + requestUri + "'. " + "Does the path have invalid encoded characters for characterEncoding '" + getRequest().getCharacterEncoding() + "'?");
                }
                this.prefixLookupPath = requestUri.substring(0, this.indexLookupPath);
                if ("/".equals(lookupPath) && !"/".equals(requestUri)) {
                    String contextPath = pathHelper.getContextPath(this);
                    if (requestUri.equals(contextPath)) {
                        this.indexLookupPath = requestUri.length();
                        this.prefixLookupPath = requestUri;
                    }
                }
            }
        }

        @Nullable
        public String resolveUrlPath(String url) {
            if (this.resourceUrlProvider == null) {
                logger.trace("ResourceUrlProvider not available via request attribute " + ResourceUrlProviderExposingInterceptor.RESOURCE_URL_PROVIDER_ATTR);
                return null;
            }
            if (this.indexLookupPath != null && url.startsWith(this.prefixLookupPath)) {
                int suffixIndex = getEndPathIndex(url);
                String suffix = url.substring(suffixIndex);
                String lookupPath = url.substring(this.indexLookupPath, suffixIndex);
                lookupPath = this.resourceUrlProvider.getForLookupPath(lookupPath);
                if (lookupPath != null) {
                    return this.prefixLookupPath + lookupPath + suffix;
                }
            }
            return null;
        }

        private int getEndPathIndex(String path) {
            int end = path.indexOf('?');
            int fragmentIndex = path.indexOf('#');
            if (fragmentIndex != -1 && (end == -1 || fragmentIndex < end)) {
                end = fragmentIndex;
            }
            if (end == -1) {
                end = path.length();
            }
            return end;
        }

    }

    private static class ResourceUrlEncodingResponseWrapper extends HttpServletResponseWrapper {

        private final ResourceUrlEncodingRequestWrapper request;

        ResourceUrlEncodingResponseWrapper(ResourceUrlEncodingRequestWrapper request, HttpServletResponse wrapped) {
            super(wrapped);
            this.request = request;
        }

        @Override
        public String encodeURL(String url) {
            String urlPath = this.request.resolveUrlPath(url);
            if (urlPath != null) {
                return super.encodeURL(urlPath);
            }
            return super.encodeURL(url);
        }

    }

}
