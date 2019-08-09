package org.springframework.web.servlet.view;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

import java.util.Locale;

public class BeanNameViewResolver extends WebApplicationObjectSupport implements ViewResolver, Ordered {

    private int order = Ordered.LOWEST_PRECEDENCE;  // default: same as non-Ordered

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    @Override
    @Nullable
    public View resolveViewName(String viewName, Locale locale) throws BeansException {
        ApplicationContext context = obtainApplicationContext();
        if (!context.containsBean(viewName)) {
            // Allow for ViewResolver chaining...
            return null;
        }
        if (!context.isTypeMatch(viewName, View.class)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Found bean named '" + viewName + "' but it does not implement View");
            }
            // Since we're looking into the general ApplicationContext here,
            // let's accept this as a non-match and allow for chaining as well...
            return null;
        }
        return context.getBean(viewName, View.class);
    }

}
