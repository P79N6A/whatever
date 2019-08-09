package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.annotation.AbstractNamedValueMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.method.support.UriComponentsContributor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.View;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

public class PathVariableMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver implements UriComponentsContributor {

    private static final TypeDescriptor STRING_TYPE_DESCRIPTOR = TypeDescriptor.valueOf(String.class);

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        if (!parameter.hasParameterAnnotation(PathVariable.class)) {
            return false;
        }
        if (Map.class.isAssignableFrom(parameter.nestedIfOptional().getNestedParameterType())) {
            PathVariable pathVariable = parameter.getParameterAnnotation(PathVariable.class);
            return (pathVariable != null && StringUtils.hasText(pathVariable.value()));
        }
        return true;
    }

    @Override
    protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
        PathVariable ann = parameter.getParameterAnnotation(PathVariable.class);
        Assert.state(ann != null, "No PathVariable annotation");
        return new PathVariableNamedValueInfo(ann);
    }

    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    protected Object resolveName(String name, MethodParameter parameter, NativeWebRequest request) throws Exception {
        Map<String, String> uriTemplateVars = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        return (uriTemplateVars != null ? uriTemplateVars.get(name) : null);
    }

    @Override
    protected void handleMissingValue(String name, MethodParameter parameter) throws ServletRequestBindingException {
        throw new MissingPathVariableException(name, parameter);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void handleResolvedValue(@Nullable Object arg, String name, MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer, NativeWebRequest request) {
        String key = View.PATH_VARIABLES;
        int scope = RequestAttributes.SCOPE_REQUEST;
        Map<String, Object> pathVars = (Map<String, Object>) request.getAttribute(key, scope);
        if (pathVars == null) {
            pathVars = new HashMap<>();
            request.setAttribute(key, pathVars, scope);
        }
        pathVars.put(name, arg);
    }

    @Override
    public void contributeMethodArgument(MethodParameter parameter, Object value, UriComponentsBuilder builder, Map<String, Object> uriVariables, ConversionService conversionService) {
        if (Map.class.isAssignableFrom(parameter.nestedIfOptional().getNestedParameterType())) {
            return;
        }
        PathVariable ann = parameter.getParameterAnnotation(PathVariable.class);
        String name = (ann != null && StringUtils.hasLength(ann.value()) ? ann.value() : parameter.getParameterName());
        String formatted = formatUriValue(conversionService, new TypeDescriptor(parameter.nestedIfOptional()), value);
        uriVariables.put(name, formatted);
    }

    @Nullable
    protected String formatUriValue(@Nullable ConversionService cs, @Nullable TypeDescriptor sourceType, Object value) {
        if (value instanceof String) {
            return (String) value;
        } else if (cs != null) {
            return (String) cs.convert(value, sourceType, STRING_TYPE_DESCRIPTOR);
        } else {
            return value.toString();
        }
    }

    private static class PathVariableNamedValueInfo extends NamedValueInfo {

        public PathVariableNamedValueInfo(PathVariable annotation) {
            super(annotation.name(), annotation.required(), ValueConstants.DEFAULT_NONE);
        }

    }

}
