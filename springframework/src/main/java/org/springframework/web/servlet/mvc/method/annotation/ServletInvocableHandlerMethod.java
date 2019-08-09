package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite;
import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.View;
import org.springframework.web.util.NestedServletException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.concurrent.Callable;

public class ServletInvocableHandlerMethod extends InvocableHandlerMethod {

    private static final Method CALLABLE_METHOD = ClassUtils.getMethod(Callable.class, "call");

    @Nullable
    private HandlerMethodReturnValueHandlerComposite returnValueHandlers;

    public ServletInvocableHandlerMethod(Object handler, Method method) {
        super(handler, method);
    }

    public ServletInvocableHandlerMethod(HandlerMethod handlerMethod) {
        super(handlerMethod);
    }

    public void setHandlerMethodReturnValueHandlers(HandlerMethodReturnValueHandlerComposite returnValueHandlers) {
        this.returnValueHandlers = returnValueHandlers;
    }

    /**
     *
     */
    public void invokeAndHandle(ServletWebRequest webRequest, ModelAndViewContainer mavContainer, Object... providedArgs) throws Exception {
        // 调用
        Object returnValue = invokeForRequest(webRequest, mavContainer, providedArgs);
        setResponseStatus(webRequest);
        if (returnValue == null) {
            if (isRequestNotModified(webRequest) || getResponseStatus() != null || mavContainer.isRequestHandled()) {
                disableContentCachingIfNecessary(webRequest);
                mavContainer.setRequestHandled(true);
                return;
            }
        } else if (StringUtils.hasText(getResponseStatusReason())) {
            mavContainer.setRequestHandled(true);
            return;
        }
        mavContainer.setRequestHandled(false);
        Assert.state(this.returnValueHandlers != null, "No return value handlers");
        try {
            // 返回值处理
            this.returnValueHandlers.handleReturnValue(returnValue, getReturnValueType(returnValue), mavContainer, webRequest);
        } catch (Exception ex) {
            if (logger.isTraceEnabled()) {
                logger.trace(formatErrorForReturnValue(returnValue), ex);
            }
            throw ex;
        }
    }

    private void setResponseStatus(ServletWebRequest webRequest) throws IOException {
        HttpStatus status = getResponseStatus();
        if (status == null) {
            return;
        }
        HttpServletResponse response = webRequest.getResponse();
        if (response != null) {
            String reason = getResponseStatusReason();
            if (StringUtils.hasText(reason)) {
                response.sendError(status.value(), reason);
            } else {
                response.setStatus(status.value());
            }
        }
        // To be picked up by RedirectView
        webRequest.getRequest().setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, status);
    }

    private boolean isRequestNotModified(ServletWebRequest webRequest) {
        return webRequest.isNotModified();
    }

    private void disableContentCachingIfNecessary(ServletWebRequest webRequest) {
        if (!isRequestNotModified(webRequest)) {
            HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
            Assert.notNull(response, "Expected HttpServletResponse");
            if (StringUtils.hasText(response.getHeader(HttpHeaders.ETAG))) {
                HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
                Assert.notNull(request, "Expected HttpServletRequest");
                ShallowEtagHeaderFilter.disableContentCaching(request);
            }
        }
    }

    private String formatErrorForReturnValue(@Nullable Object returnValue) {
        return "Error handling return value=[" + returnValue + "]" + (returnValue != null ? ", type=" + returnValue.getClass().getName() : "") + " in " + toString();
    }

    ServletInvocableHandlerMethod wrapConcurrentResult(Object result) {
        return new ConcurrentResultHandlerMethod(result, new ConcurrentResultMethodParameter(result));
    }

    private class ConcurrentResultHandlerMethod extends ServletInvocableHandlerMethod {

        private final MethodParameter returnType;

        public ConcurrentResultHandlerMethod(final Object result, ConcurrentResultMethodParameter returnType) {
            super((Callable<Object>) () -> {
                if (result instanceof Exception) {
                    throw (Exception) result;
                } else if (result instanceof Throwable) {
                    throw new NestedServletException("Async processing failed", (Throwable) result);
                }
                return result;
            }, CALLABLE_METHOD);
            if (ServletInvocableHandlerMethod.this.returnValueHandlers != null) {
                setHandlerMethodReturnValueHandlers(ServletInvocableHandlerMethod.this.returnValueHandlers);
            }
            this.returnType = returnType;
        }

        @Override
        public Class<?> getBeanType() {
            return ServletInvocableHandlerMethod.this.getBeanType();
        }

        @Override
        public MethodParameter getReturnValueType(@Nullable Object returnValue) {
            return this.returnType;
        }

        @Override
        public <A extends Annotation> A getMethodAnnotation(Class<A> annotationType) {
            return ServletInvocableHandlerMethod.this.getMethodAnnotation(annotationType);
        }

        @Override
        public <A extends Annotation> boolean hasMethodAnnotation(Class<A> annotationType) {
            return ServletInvocableHandlerMethod.this.hasMethodAnnotation(annotationType);
        }

    }

    private class ConcurrentResultMethodParameter extends HandlerMethodParameter {

        @Nullable
        private final Object returnValue;

        private final ResolvableType returnType;

        public ConcurrentResultMethodParameter(Object returnValue) {
            super(-1);
            this.returnValue = returnValue;
            this.returnType = (returnValue instanceof ReactiveTypeHandler.CollectedValuesList ? ((ReactiveTypeHandler.CollectedValuesList) returnValue).getReturnType() : ResolvableType.forType(super.getGenericParameterType()).getGeneric());
        }

        public ConcurrentResultMethodParameter(ConcurrentResultMethodParameter original) {
            super(original);
            this.returnValue = original.returnValue;
            this.returnType = original.returnType;
        }

        @Override
        public Class<?> getParameterType() {
            if (this.returnValue != null) {
                return this.returnValue.getClass();
            }
            if (!ResolvableType.NONE.equals(this.returnType)) {
                return this.returnType.toClass();
            }
            return super.getParameterType();
        }

        @Override
        public Type getGenericParameterType() {
            return this.returnType.getType();
        }

        @Override
        public <T extends Annotation> boolean hasMethodAnnotation(Class<T> annotationType) {
            // Ensure @ResponseBody-style handling for values collected from a reactive type
            // even if actual return type is ResponseEntity<Flux<T>>
            return (super.hasMethodAnnotation(annotationType) || (annotationType == ResponseBody.class && this.returnValue instanceof ReactiveTypeHandler.CollectedValuesList));
        }

        @Override
        public ConcurrentResultMethodParameter clone() {
            return new ConcurrentResultMethodParameter(this);
        }

    }

}
