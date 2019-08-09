package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.web.method.ControllerAdviceBean;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class RequestResponseBodyAdviceChain implements RequestBodyAdvice, ResponseBodyAdvice<Object> {

    private final List<Object> requestBodyAdvice = new ArrayList<>(4);

    private final List<Object> responseBodyAdvice = new ArrayList<>(4);

    public RequestResponseBodyAdviceChain(@Nullable List<Object> requestResponseBodyAdvice) {
        this.requestBodyAdvice.addAll(getAdviceByType(requestResponseBodyAdvice, RequestBodyAdvice.class));
        this.responseBodyAdvice.addAll(getAdviceByType(requestResponseBodyAdvice, ResponseBodyAdvice.class));
    }

    @SuppressWarnings("unchecked")
    static <T> List<T> getAdviceByType(@Nullable List<Object> requestResponseBodyAdvice, Class<T> adviceType) {
        if (requestResponseBodyAdvice != null) {
            List<T> result = new ArrayList<>();
            for (Object advice : requestResponseBodyAdvice) {
                Class<?> beanType = (advice instanceof ControllerAdviceBean ? ((ControllerAdviceBean) advice).getBeanType() : advice.getClass());
                if (beanType != null && adviceType.isAssignableFrom(beanType)) {
                    result.add((T) advice);
                }
            }
            return result;
        }
        return Collections.emptyList();
    }

    @Override
    public boolean supports(MethodParameter param, Type type, Class<? extends HttpMessageConverter<?>> converterType) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage request, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        for (RequestBodyAdvice advice : getMatchingAdvice(parameter, RequestBodyAdvice.class)) {
            if (advice.supports(parameter, targetType, converterType)) {
                request = advice.beforeBodyRead(request, parameter, targetType, converterType);
            }
        }
        return request;
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        for (RequestBodyAdvice advice : getMatchingAdvice(parameter, RequestBodyAdvice.class)) {
            if (advice.supports(parameter, targetType, converterType)) {
                body = advice.afterBodyRead(body, inputMessage, parameter, targetType, converterType);
            }
        }
        return body;
    }

    @Override
    @Nullable
    public Object beforeBodyWrite(@Nullable Object body, MethodParameter returnType, MediaType contentType, Class<? extends HttpMessageConverter<?>> converterType, ServerHttpRequest request, ServerHttpResponse response) {
        return processBody(body, returnType, contentType, converterType, request, response);
    }

    @Override
    @Nullable
    public Object handleEmptyBody(@Nullable Object body, HttpInputMessage inputMessage, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        for (RequestBodyAdvice advice : getMatchingAdvice(parameter, RequestBodyAdvice.class)) {
            if (advice.supports(parameter, targetType, converterType)) {
                body = advice.handleEmptyBody(body, inputMessage, parameter, targetType, converterType);
            }
        }
        return body;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private <T> Object processBody(@Nullable Object body, MethodParameter returnType, MediaType contentType, Class<? extends HttpMessageConverter<?>> converterType, ServerHttpRequest request, ServerHttpResponse response) {
        for (ResponseBodyAdvice<?> advice : getMatchingAdvice(returnType, ResponseBodyAdvice.class)) {
            if (advice.supports(returnType, converterType)) {
                body = ((ResponseBodyAdvice<T>) advice).beforeBodyWrite((T) body, returnType, contentType, converterType, request, response);
            }
        }
        return body;
    }

    @SuppressWarnings("unchecked")
    private <A> List<A> getMatchingAdvice(MethodParameter parameter, Class<? extends A> adviceType) {
        List<Object> availableAdvice = getAdvice(adviceType);
        if (CollectionUtils.isEmpty(availableAdvice)) {
            return Collections.emptyList();
        }
        List<A> result = new ArrayList<>(availableAdvice.size());
        for (Object advice : availableAdvice) {
            if (advice instanceof ControllerAdviceBean) {
                ControllerAdviceBean adviceBean = (ControllerAdviceBean) advice;
                if (!adviceBean.isApplicableToBeanType(parameter.getContainingClass())) {
                    continue;
                }
                advice = adviceBean.resolveBean();
            }
            if (adviceType.isAssignableFrom(advice.getClass())) {
                result.add((A) advice);
            }
        }
        return result;
    }

    private List<Object> getAdvice(Class<?> adviceType) {
        if (RequestBodyAdvice.class == adviceType) {
            return this.requestBodyAdvice;
        } else if (ResponseBodyAdvice.class == adviceType) {
            return this.responseBodyAdvice;
        } else {
            throw new IllegalArgumentException("Unexpected adviceType: " + adviceType);
        }
    }

}
