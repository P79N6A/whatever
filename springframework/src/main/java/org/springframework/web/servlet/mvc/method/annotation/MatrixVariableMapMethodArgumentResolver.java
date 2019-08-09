package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.*;
import org.springframework.web.bind.annotation.MatrixVariable;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MatrixVariableMapMethodArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        MatrixVariable matrixVariable = parameter.getParameterAnnotation(MatrixVariable.class);
        return (matrixVariable != null && Map.class.isAssignableFrom(parameter.getParameterType()) && !StringUtils.hasText(matrixVariable.name()));
    }

    @Override
    @Nullable
    public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer, NativeWebRequest request, @Nullable WebDataBinderFactory binderFactory) throws Exception {
        @SuppressWarnings("unchecked") Map<String, MultiValueMap<String, String>> matrixVariables = (Map<String, MultiValueMap<String, String>>) request.getAttribute(HandlerMapping.MATRIX_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (CollectionUtils.isEmpty(matrixVariables)) {
            return Collections.emptyMap();
        }
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        MatrixVariable ann = parameter.getParameterAnnotation(MatrixVariable.class);
        Assert.state(ann != null, "No MatrixVariable annotation");
        String pathVariable = ann.pathVar();
        if (!pathVariable.equals(ValueConstants.DEFAULT_NONE)) {
            MultiValueMap<String, String> mapForPathVariable = matrixVariables.get(pathVariable);
            if (mapForPathVariable == null) {
                return Collections.emptyMap();
            }
            map.putAll(mapForPathVariable);
        } else {
            for (MultiValueMap<String, String> vars : matrixVariables.values()) {
                vars.forEach((name, values) -> {
                    for (String value : values) {
                        map.add(name, value);
                    }
                });
            }
        }
        return (isSingleValueMap(parameter) ? map.toSingleValueMap() : map);
    }

    private boolean isSingleValueMap(MethodParameter parameter) {
        if (!MultiValueMap.class.isAssignableFrom(parameter.getParameterType())) {
            ResolvableType[] genericTypes = ResolvableType.forMethodParameter(parameter).getGenerics();
            if (genericTypes.length == 2) {
                return !List.class.isAssignableFrom(genericTypes[1].toClass());
            }
        }
        return false;
    }

}
