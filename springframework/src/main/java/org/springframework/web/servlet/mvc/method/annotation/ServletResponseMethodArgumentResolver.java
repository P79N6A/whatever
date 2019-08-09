package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.ServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

public class ServletResponseMethodArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        Class<?> paramType = parameter.getParameterType();
        return (ServletResponse.class.isAssignableFrom(paramType) || OutputStream.class.isAssignableFrom(paramType) || Writer.class.isAssignableFrom(paramType));
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer, NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {
        if (mavContainer != null) {
            mavContainer.setRequestHandled(true);
        }
        Class<?> paramType = parameter.getParameterType();
        // ServletResponse, HttpServletResponse
        if (ServletResponse.class.isAssignableFrom(paramType)) {
            return resolveNativeResponse(webRequest, paramType);
        }
        // ServletResponse required for all further argument types
        return resolveArgument(paramType, resolveNativeResponse(webRequest, ServletResponse.class));
    }

    private <T> T resolveNativeResponse(NativeWebRequest webRequest, Class<T> requiredType) {
        T nativeResponse = webRequest.getNativeResponse(requiredType);
        if (nativeResponse == null) {
            throw new IllegalStateException("Current response is not of type [" + requiredType.getName() + "]: " + webRequest);
        }
        return nativeResponse;
    }

    private Object resolveArgument(Class<?> paramType, ServletResponse response) throws IOException {
        if (OutputStream.class.isAssignableFrom(paramType)) {
            return response.getOutputStream();
        } else if (Writer.class.isAssignableFrom(paramType)) {
            return response.getWriter();
        }
        // Should never happen...
        throw new UnsupportedOperationException("Unknown parameter type: " + paramType);
    }

}
