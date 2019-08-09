package org.springframework.web.method.support;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;

public interface AsyncHandlerMethodReturnValueHandler extends HandlerMethodReturnValueHandler {

    boolean isAsyncReturnValue(@Nullable Object returnValue, MethodParameter returnType);

}
