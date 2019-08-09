package org.springframework.web.bind.support;

import org.springframework.lang.Nullable;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.context.request.NativeWebRequest;

public interface WebDataBinderFactory {

    WebDataBinder createBinder(NativeWebRequest webRequest, @Nullable Object target, String objectName) throws Exception;

}
