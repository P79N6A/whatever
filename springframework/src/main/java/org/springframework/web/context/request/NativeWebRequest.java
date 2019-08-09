package org.springframework.web.context.request;

import org.springframework.lang.Nullable;

public interface NativeWebRequest extends WebRequest {

    Object getNativeRequest();

    @Nullable
    Object getNativeResponse();

    @Nullable
    <T> T getNativeRequest(@Nullable Class<T> requiredType);

    @Nullable
    <T> T getNativeResponse(@Nullable Class<T> requiredType);

}
