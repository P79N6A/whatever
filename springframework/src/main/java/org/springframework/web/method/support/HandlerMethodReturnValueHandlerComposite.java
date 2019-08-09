package org.springframework.web.method.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HandlerMethodReturnValueHandlerComposite implements HandlerMethodReturnValueHandler {

    protected final Log logger = LogFactory.getLog(getClass());

    private final List<HandlerMethodReturnValueHandler> returnValueHandlers = new ArrayList<>();

    public List<HandlerMethodReturnValueHandler> getHandlers() {
        return Collections.unmodifiableList(this.returnValueHandlers);
    }

    @Override
    public boolean supportsReturnType(MethodParameter returnType) {
        return getReturnValueHandler(returnType) != null;
    }

    @Nullable
    private HandlerMethodReturnValueHandler getReturnValueHandler(MethodParameter returnType) {
        for (HandlerMethodReturnValueHandler handler : this.returnValueHandlers) {
            if (handler.supportsReturnType(returnType)) {
                return handler;
            }
        }
        return null;
    }

    @Override
    public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType, ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {
        // 这里处理@ResponseBody，返回RequestResponseBodyMethodProcessor
        HandlerMethodReturnValueHandler handler = selectHandler(returnValue, returnType);
        if (handler == null) {
            throw new IllegalArgumentException("Unknown return value type: " + returnType.getParameterType().getName());
        }
        // 处理
        handler.handleReturnValue(returnValue, returnType, mavContainer, webRequest);
    }

    @Nullable
    private HandlerMethodReturnValueHandler selectHandler(@Nullable Object value, MethodParameter returnType) {
        boolean isAsyncValue = isAsyncReturnValue(value, returnType);
        for (HandlerMethodReturnValueHandler handler : this.returnValueHandlers) {
            if (isAsyncValue && !(handler instanceof AsyncHandlerMethodReturnValueHandler)) {
                continue;
            }
            if (handler.supportsReturnType(returnType)) {
                return handler;
            }
        }
        return null;
    }

    private boolean isAsyncReturnValue(@Nullable Object value, MethodParameter returnType) {
        for (HandlerMethodReturnValueHandler handler : this.returnValueHandlers) {
            if (handler instanceof AsyncHandlerMethodReturnValueHandler && ((AsyncHandlerMethodReturnValueHandler) handler).isAsyncReturnValue(value, returnType)) {
                return true;
            }
        }
        return false;
    }

    public HandlerMethodReturnValueHandlerComposite addHandler(HandlerMethodReturnValueHandler handler) {
        this.returnValueHandlers.add(handler);
        return this;
    }

    public HandlerMethodReturnValueHandlerComposite addHandlers(@Nullable List<? extends HandlerMethodReturnValueHandler> handlers) {
        if (handlers != null) {
            this.returnValueHandlers.addAll(handlers);
        }
        return this;
    }

}
