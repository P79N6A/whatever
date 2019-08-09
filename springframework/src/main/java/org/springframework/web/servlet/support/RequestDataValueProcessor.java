package org.springframework.web.servlet.support;

import org.springframework.lang.Nullable;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public interface RequestDataValueProcessor {

    String processAction(HttpServletRequest request, String action, String httpMethod);

    String processFormFieldValue(HttpServletRequest request, @Nullable String name, String value, String type);

    @Nullable
    Map<String, String> getExtraHiddenFields(HttpServletRequest request);

    String processUrl(HttpServletRequest request, String url);

}
