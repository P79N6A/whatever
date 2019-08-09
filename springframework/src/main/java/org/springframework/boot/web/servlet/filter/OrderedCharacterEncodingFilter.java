package org.springframework.boot.web.servlet.filter;

import org.springframework.core.Ordered;
import org.springframework.web.filter.CharacterEncodingFilter;

public class OrderedCharacterEncodingFilter extends CharacterEncodingFilter implements OrderedFilter {

    private int order = Ordered.HIGHEST_PRECEDENCE;

    @Override
    public int getOrder() {
        return this.order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

}
