package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.ServletRequest;
import java.util.Map;

public class ExtendedServletRequestDataBinder extends ServletRequestDataBinder {

    public ExtendedServletRequestDataBinder(@Nullable Object target) {
        super(target);
    }

    public ExtendedServletRequestDataBinder(@Nullable Object target, String objectName) {
        super(target, objectName);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void addBindValues(MutablePropertyValues mpvs, ServletRequest request) {
        String attr = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
        Map<String, String> uriVars = (Map<String, String>) request.getAttribute(attr);
        if (uriVars != null) {
            uriVars.forEach((name, value) -> {
                if (mpvs.contains(name)) {
                    if (logger.isWarnEnabled()) {
                        logger.warn("Skipping URI variable '" + name + "' because request contains bind value with same name.");
                    }
                } else {
                    mpvs.addPropertyValue(name, value);
                }
            });
        }
    }

}
