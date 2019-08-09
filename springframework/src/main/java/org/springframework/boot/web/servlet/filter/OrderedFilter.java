package org.springframework.boot.web.servlet.filter;

import org.springframework.core.Ordered;

import javax.servlet.Filter;

public interface OrderedFilter extends Filter, Ordered {

    int REQUEST_WRAPPER_FILTER_MAX_ORDER = 0;

}
