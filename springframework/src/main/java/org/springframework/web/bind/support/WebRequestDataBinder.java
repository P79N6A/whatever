package org.springframework.web.bind.support;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

public class WebRequestDataBinder extends WebDataBinder {

    public WebRequestDataBinder(@Nullable Object target) {
        super(target);
    }

    public WebRequestDataBinder(@Nullable Object target, String objectName) {
        super(target, objectName);
    }

    public void bind(WebRequest request) {
        MutablePropertyValues mpvs = new MutablePropertyValues(request.getParameterMap());
        if (isMultipartRequest(request) && request instanceof NativeWebRequest) {
            MultipartRequest multipartRequest = ((NativeWebRequest) request).getNativeRequest(MultipartRequest.class);
            if (multipartRequest != null) {
                bindMultipart(multipartRequest.getMultiFileMap(), mpvs);
            } else {
                HttpServletRequest servletRequest = ((NativeWebRequest) request).getNativeRequest(HttpServletRequest.class);
                if (servletRequest != null) {
                    bindParts(servletRequest, mpvs);
                }
            }
        }
        doBind(mpvs);
    }

    private boolean isMultipartRequest(WebRequest request) {
        String contentType = request.getHeader("Content-Type");
        return (contentType != null && StringUtils.startsWithIgnoreCase(contentType, "multipart"));
    }

    private void bindParts(HttpServletRequest request, MutablePropertyValues mpvs) {
        try {
            MultiValueMap<String, Part> map = new LinkedMultiValueMap<>();
            for (Part part : request.getParts()) {
                map.add(part.getName(), part);
            }
            map.forEach((key, values) -> {
                if (values.size() == 1) {
                    Part part = values.get(0);
                    if (isBindEmptyMultipartFiles() || part.getSize() > 0) {
                        mpvs.add(key, part);
                    }
                } else {
                    mpvs.add(key, values);
                }
            });
        } catch (Exception ex) {
            throw new MultipartException("Failed to get request parts", ex);
        }
    }

    public void closeNoCatch() throws BindException {
        if (getBindingResult().hasErrors()) {
            throw new BindException(getBindingResult());
        }
    }

}
