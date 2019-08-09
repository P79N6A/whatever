package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.annotation.AbstractNamedValueMethodArgumentResolver;

import javax.servlet.ServletException;

public class RequestAttributeMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(RequestAttribute.class);
    }

    @Override
    protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
        RequestAttribute ann = parameter.getParameterAnnotation(RequestAttribute.class);
        Assert.state(ann != null, "No RequestAttribute annotation");
        return new NamedValueInfo(ann.name(), ann.required(), ValueConstants.DEFAULT_NONE);
    }

    @Override
    @Nullable
    protected Object resolveName(String name, MethodParameter parameter, NativeWebRequest request) {
        return request.getAttribute(name, RequestAttributes.SCOPE_REQUEST);
    }

    @Override
    protected void handleMissingValue(String name, MethodParameter parameter) throws ServletException {
        throw new ServletRequestBindingException("Missing request attribute '" + name + "' of type " + parameter.getNestedParameterType().getSimpleName());
    }

}
