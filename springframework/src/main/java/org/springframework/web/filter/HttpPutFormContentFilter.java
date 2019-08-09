package org.springframework.web.filter;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.*;

@Deprecated
public class HttpPutFormContentFilter extends OncePerRequestFilter {

    private FormHttpMessageConverter formConverter = new AllEncompassingFormHttpMessageConverter();

    public void setFormConverter(FormHttpMessageConverter converter) {
        Assert.notNull(converter, "FormHttpMessageConverter is required.");
        this.formConverter = converter;
    }

    public FormHttpMessageConverter getFormConverter() {
        return this.formConverter;
    }

    public void setCharset(Charset charset) {
        this.formConverter.setCharset(charset);
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (("PUT".equals(request.getMethod()) || "PATCH".equals(request.getMethod())) && isFormContentType(request)) {
            HttpInputMessage inputMessage = new ServletServerHttpRequest(request) {
                @Override
                public InputStream getBody() throws IOException {
                    return request.getInputStream();
                }
            };
            MultiValueMap<String, String> formParameters = this.formConverter.read(null, inputMessage);
            if (!formParameters.isEmpty()) {
                HttpServletRequest wrapper = new HttpPutFormContentRequestWrapper(request, formParameters);
                filterChain.doFilter(wrapper, response);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isFormContentType(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (contentType != null) {
            try {
                MediaType mediaType = MediaType.parseMediaType(contentType);
                return (MediaType.APPLICATION_FORM_URLENCODED.includes(mediaType));
            } catch (IllegalArgumentException ex) {
                return false;
            }
        } else {
            return false;
        }
    }

    private static class HttpPutFormContentRequestWrapper extends HttpServletRequestWrapper {

        private MultiValueMap<String, String> formParameters;

        public HttpPutFormContentRequestWrapper(HttpServletRequest request, MultiValueMap<String, String> parameters) {
            super(request);
            this.formParameters = parameters;
        }

        @Override
        @Nullable
        public String getParameter(String name) {
            String queryStringValue = super.getParameter(name);
            String formValue = this.formParameters.getFirst(name);
            return (queryStringValue != null ? queryStringValue : formValue);
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            Map<String, String[]> result = new LinkedHashMap<>();
            Enumeration<String> names = getParameterNames();
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                result.put(name, getParameterValues(name));
            }
            return result;
        }

        @Override
        public Enumeration<String> getParameterNames() {
            Set<String> names = new LinkedHashSet<>();
            names.addAll(Collections.list(super.getParameterNames()));
            names.addAll(this.formParameters.keySet());
            return Collections.enumeration(names);
        }

        @Override
        @Nullable
        public String[] getParameterValues(String name) {
            String[] parameterValues = super.getParameterValues(name);
            List<String> formParam = this.formParameters.get(name);
            if (formParam == null) {
                return parameterValues;
            }
            if (parameterValues == null || getQueryString() == null) {
                return StringUtils.toStringArray(formParam);
            } else {
                List<String> result = new ArrayList<>(parameterValues.length + formParam.size());
                result.addAll(Arrays.asList(parameterValues));
                result.addAll(formParam);
                return StringUtils.toStringArray(result);
            }
        }

    }

}
