package org.springframework.web.method.support;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

public interface UriComponentsContributor {

    boolean supportsParameter(MethodParameter parameter);

    void contributeMethodArgument(MethodParameter parameter, Object value, UriComponentsBuilder builder, Map<String, Object> uriVariables, ConversionService conversionService);

}
