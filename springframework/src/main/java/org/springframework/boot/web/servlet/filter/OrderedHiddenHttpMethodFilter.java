package org.springframework.boot.web.servlet.filter;

import org.springframework.web.filter.HiddenHttpMethodFilter;

public class OrderedHiddenHttpMethodFilter extends HiddenHttpMethodFilter implements OrderedFilter {

    public static final int DEFAULT_ORDER = REQUEST_WRAPPER_FILTER_MAX_ORDER - 10000;

    private int order = DEFAULT_ORDER;

    @Override
    public int getOrder() {
        return this.order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

}
