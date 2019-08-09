package org.springframework.web.servlet.mvc.condition;

import org.springframework.lang.Nullable;

public interface NameValueExpression<T> {

    String getName();

    @Nullable
    T getValue();

    boolean isNegated();

}
