package org.springframework.web.multipart.support;

import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

public class DefaultMultipartHttpServletRequest extends AbstractMultipartHttpServletRequest {

    private static final String CONTENT_TYPE = "Content-Type";

    @Nullable
    private Map<String, String[]> multipartParameters;

    @Nullable
    private Map<String, String> multipartParameterContentTypes;

    public DefaultMultipartHttpServletRequest(HttpServletRequest request, MultiValueMap<String, MultipartFile> mpFiles, Map<String, String[]> mpParams, Map<String, String> mpParamContentTypes) {
        super(request);
        setMultipartFiles(mpFiles);
        setMultipartParameters(mpParams);
        setMultipartParameterContentTypes(mpParamContentTypes);
    }

    public DefaultMultipartHttpServletRequest(HttpServletRequest request) {
        super(request);
    }

    @Override
    @Nullable
    public String getParameter(String name) {
        String[] values = getMultipartParameters().get(name);
        if (values != null) {
            return (values.length > 0 ? values[0] : null);
        }
        return super.getParameter(name);
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] parameterValues = super.getParameterValues(name);
        String[] mpValues = getMultipartParameters().get(name);
        if (mpValues == null) {
            return parameterValues;
        }
        if (parameterValues == null || getQueryString() == null) {
            return mpValues;
        } else {
            String[] result = new String[mpValues.length + parameterValues.length];
            System.arraycopy(mpValues, 0, result, 0, mpValues.length);
            System.arraycopy(parameterValues, 0, result, mpValues.length, parameterValues.length);
            return result;
        }
    }

    @Override
    public Enumeration<String> getParameterNames() {
        Map<String, String[]> multipartParameters = getMultipartParameters();
        if (multipartParameters.isEmpty()) {
            return super.getParameterNames();
        }
        Set<String> paramNames = new LinkedHashSet<>();
        paramNames.addAll(Collections.list(super.getParameterNames()));
        paramNames.addAll(multipartParameters.keySet());
        return Collections.enumeration(paramNames);
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
    public String getMultipartContentType(String paramOrFileName) {
        MultipartFile file = getFile(paramOrFileName);
        if (file != null) {
            return file.getContentType();
        } else {
            return getMultipartParameterContentTypes().get(paramOrFileName);
        }
    }

    @Override
    public HttpHeaders getMultipartHeaders(String paramOrFileName) {
        String contentType = getMultipartContentType(paramOrFileName);
        if (contentType != null) {
            HttpHeaders headers = new HttpHeaders();
            headers.add(CONTENT_TYPE, contentType);
            return headers;
        } else {
            return null;
        }
    }

    protected final void setMultipartParameters(Map<String, String[]> multipartParameters) {
        this.multipartParameters = multipartParameters;
    }

    protected Map<String, String[]> getMultipartParameters() {
        if (this.multipartParameters == null) {
            initializeMultipart();
        }
        return this.multipartParameters;
    }

    protected final void setMultipartParameterContentTypes(Map<String, String> multipartParameterContentTypes) {
        this.multipartParameterContentTypes = multipartParameterContentTypes;
    }

    protected Map<String, String> getMultipartParameterContentTypes() {
        if (this.multipartParameterContentTypes == null) {
            initializeMultipart();
        }
        return this.multipartParameterContentTypes;
    }

}
