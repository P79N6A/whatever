package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.multipart.support.MultipartResolutionDelegate;
import org.springframework.web.multipart.support.RequestPartServletServerHttpRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public class RequestPartMethodArgumentResolver extends AbstractMessageConverterMethodArgumentResolver {

    public RequestPartMethodArgumentResolver(List<HttpMessageConverter<?>> messageConverters) {
        super(messageConverters);
    }

    public RequestPartMethodArgumentResolver(List<HttpMessageConverter<?>> messageConverters, List<Object> requestResponseBodyAdvice) {
        super(messageConverters, requestResponseBodyAdvice);
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        if (parameter.hasParameterAnnotation(RequestPart.class)) {
            return true;
        } else {
            if (parameter.hasParameterAnnotation(RequestParam.class)) {
                return false;
            }
            return MultipartResolutionDelegate.isMultipartArgument(parameter.nestedIfOptional());
        }
    }

    @Override
    @Nullable
    public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer, NativeWebRequest request, @Nullable WebDataBinderFactory binderFactory) throws Exception {
        HttpServletRequest servletRequest = request.getNativeRequest(HttpServletRequest.class);
        Assert.state(servletRequest != null, "No HttpServletRequest");
        RequestPart requestPart = parameter.getParameterAnnotation(RequestPart.class);
        boolean isRequired = ((requestPart == null || requestPart.required()) && !parameter.isOptional());
        String name = getPartName(parameter, requestPart);
        parameter = parameter.nestedIfOptional();
        Object arg = null;
        Object mpArg = MultipartResolutionDelegate.resolveMultipartArgument(name, parameter, servletRequest);
        if (mpArg != MultipartResolutionDelegate.UNRESOLVABLE) {
            arg = mpArg;
        } else {
            try {
                HttpInputMessage inputMessage = new RequestPartServletServerHttpRequest(servletRequest, name);
                arg = readWithMessageConverters(inputMessage, parameter, parameter.getNestedGenericParameterType());
                if (binderFactory != null) {
                    WebDataBinder binder = binderFactory.createBinder(request, arg, name);
                    if (arg != null) {
                        validateIfApplicable(binder, parameter);
                        if (binder.getBindingResult().hasErrors() && isBindExceptionRequired(binder, parameter)) {
                            throw new MethodArgumentNotValidException(parameter, binder.getBindingResult());
                        }
                    }
                    if (mavContainer != null) {
                        mavContainer.addAttribute(BindingResult.MODEL_KEY_PREFIX + name, binder.getBindingResult());
                    }
                }
            } catch (MissingServletRequestPartException | MultipartException ex) {
                if (isRequired) {
                    throw ex;
                }
            }
        }
        if (arg == null && isRequired) {
            if (!MultipartResolutionDelegate.isMultipartRequest(servletRequest)) {
                throw new MultipartException("Current request is not a multipart request");
            } else {
                throw new MissingServletRequestPartException(name);
            }
        }
        return adaptArgumentIfNecessary(arg, parameter);
    }

    private String getPartName(MethodParameter methodParam, @Nullable RequestPart requestPart) {
        String partName = (requestPart != null ? requestPart.name() : "");
        if (partName.isEmpty()) {
            partName = methodParam.getParameterName();
            if (partName == null) {
                throw new IllegalArgumentException("Request part name for argument type [" + methodParam.getNestedParameterType().getName() + "] not specified, and parameter name information not found in class file either.");
            }
        }
        return partName;
    }

}
