package org.springframework.web.multipart.support;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class MultipartFilter extends OncePerRequestFilter {

    public static final String DEFAULT_MULTIPART_RESOLVER_BEAN_NAME = "filterMultipartResolver";

    private final MultipartResolver defaultMultipartResolver = new StandardServletMultipartResolver();

    private String multipartResolverBeanName = DEFAULT_MULTIPART_RESOLVER_BEAN_NAME;

    public void setMultipartResolverBeanName(String multipartResolverBeanName) {
        this.multipartResolverBeanName = multipartResolverBeanName;
    }

    protected String getMultipartResolverBeanName() {
        return this.multipartResolverBeanName;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        MultipartResolver multipartResolver = lookupMultipartResolver(request);
        HttpServletRequest processedRequest = request;
        if (multipartResolver.isMultipart(processedRequest)) {
            if (logger.isTraceEnabled()) {
                logger.trace("Resolving multipart request");
            }
            processedRequest = multipartResolver.resolveMultipart(processedRequest);
        } else {
            // A regular request...
            if (logger.isTraceEnabled()) {
                logger.trace("Not a multipart request");
            }
        }
        try {
            filterChain.doFilter(processedRequest, response);
        } finally {
            if (processedRequest instanceof MultipartHttpServletRequest) {
                multipartResolver.cleanupMultipart((MultipartHttpServletRequest) processedRequest);
            }
        }
    }

    protected MultipartResolver lookupMultipartResolver(HttpServletRequest request) {
        return lookupMultipartResolver();
    }

    protected MultipartResolver lookupMultipartResolver() {
        WebApplicationContext wac = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
        String beanName = getMultipartResolverBeanName();
        if (wac != null && wac.containsBean(beanName)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Using MultipartResolver '" + beanName + "' for MultipartFilter");
            }
            return wac.getBean(beanName, MultipartResolver.class);
        } else {
            return this.defaultMultipartResolver;
        }
    }

}
