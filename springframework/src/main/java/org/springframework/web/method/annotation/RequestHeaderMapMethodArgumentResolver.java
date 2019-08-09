package org.springframework.web.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class RequestHeaderMapMethodArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return (parameter.hasParameterAnnotation(RequestHeader.class) && Map.class.isAssignableFrom(parameter.getParameterType()));
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer, NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {
        Class<?> paramType = parameter.getParameterType();
        if (MultiValueMap.class.isAssignableFrom(paramType)) {
            MultiValueMap<String, String> result;
            if (HttpHeaders.class.isAssignableFrom(paramType)) {
                result = new HttpHeaders();
            } else {
                result = new LinkedMultiValueMap<>();
            }
            for (Iterator<String> iterator = webRequest.getHeaderNames(); iterator.hasNext(); ) {
                String headerName = iterator.next();
                String[] headerValues = webRequest.getHeaderValues(headerName);
                if (headerValues != null) {
                    for (String headerValue : headerValues) {
                        result.add(headerName, headerValue);
                    }
                }
            }
            return result;
        } else {
            Map<String, String> result = new LinkedHashMap<>();
            for (Iterator<String> iterator = webRequest.getHeaderNames(); iterator.hasNext(); ) {
                String headerName = iterator.next();
                String headerValue = webRequest.getHeader(headerName);
                if (headerValue != null) {
                    result.put(headerName, headerValue);
                }
            }
            return result;
        }
    }

}
