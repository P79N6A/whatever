package org.springframework.web.bind.support;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.web.context.request.NativeWebRequest;

@FunctionalInterface
public interface WebArgumentResolver {

    Object UNRESOLVED = new Object();

    @Nullable
    Object resolveArgument(MethodParameter methodParameter, NativeWebRequest webRequest) throws Exception;

}
