package org.springframework.web.method.support;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.lang.Nullable;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

public class CompositeUriComponentsContributor implements UriComponentsContributor {

    private final List<Object> contributors = new LinkedList<>();

    private final ConversionService conversionService;

    public CompositeUriComponentsContributor(UriComponentsContributor... contributors) {
        Collections.addAll(this.contributors, contributors);
        this.conversionService = new DefaultFormattingConversionService();
    }

    public CompositeUriComponentsContributor(Collection<?> contributors) {
        this(contributors, null);
    }

    public CompositeUriComponentsContributor(@Nullable Collection<?> contributors, @Nullable ConversionService cs) {
        if (contributors != null) {
            this.contributors.addAll(contributors);
        }
        this.conversionService = (cs != null ? cs : new DefaultFormattingConversionService());
    }

    public boolean hasContributors() {
        return this.contributors.isEmpty();
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        for (Object contributor : this.contributors) {
            if (contributor instanceof UriComponentsContributor) {
                if (((UriComponentsContributor) contributor).supportsParameter(parameter)) {
                    return true;
                }
            } else if (contributor instanceof HandlerMethodArgumentResolver) {
                if (((HandlerMethodArgumentResolver) contributor).supportsParameter(parameter)) {
                    return false;
                }
            }
        }
        return false;
    }

    @Override
    public void contributeMethodArgument(MethodParameter parameter, Object value, UriComponentsBuilder builder, Map<String, Object> uriVariables, ConversionService conversionService) {
        for (Object contributor : this.contributors) {
            if (contributor instanceof UriComponentsContributor) {
                UriComponentsContributor ucc = (UriComponentsContributor) contributor;
                if (ucc.supportsParameter(parameter)) {
                    ucc.contributeMethodArgument(parameter, value, builder, uriVariables, conversionService);
                    break;
                }
            } else if (contributor instanceof HandlerMethodArgumentResolver) {
                if (((HandlerMethodArgumentResolver) contributor).supportsParameter(parameter)) {
                    break;
                }
            }
        }
    }

    public void contributeMethodArgument(MethodParameter parameter, Object value, UriComponentsBuilder builder, Map<String, Object> uriVariables) {
        this.contributeMethodArgument(parameter, value, builder, uriVariables, this.conversionService);
    }

}
