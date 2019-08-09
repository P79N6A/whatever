package org.springframework.web.bind.support;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.context.request.WebRequest;

public interface WebBindingInitializer {

    void initBinder(WebDataBinder binder);

    @Deprecated
    default void initBinder(WebDataBinder binder, WebRequest request) {
        initBinder(binder);
    }

}
