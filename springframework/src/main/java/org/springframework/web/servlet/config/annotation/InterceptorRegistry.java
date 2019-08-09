package org.springframework.web.servlet.config.annotation;

import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.WebRequestHandlerInterceptorAdapter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class InterceptorRegistry {

    private final List<InterceptorRegistration> registrations = new ArrayList<>();

    public InterceptorRegistration addInterceptor(HandlerInterceptor interceptor) {
        InterceptorRegistration registration = new InterceptorRegistration(interceptor);
        this.registrations.add(registration);
        return registration;
    }

    public InterceptorRegistration addWebRequestInterceptor(WebRequestInterceptor interceptor) {
        WebRequestHandlerInterceptorAdapter adapted = new WebRequestHandlerInterceptorAdapter(interceptor);
        InterceptorRegistration registration = new InterceptorRegistration(adapted);
        this.registrations.add(registration);
        return registration;
    }

    protected List<Object> getInterceptors() {
        return this.registrations.stream().sorted(INTERCEPTOR_ORDER_COMPARATOR).map(InterceptorRegistration::getInterceptor).collect(Collectors.toList());
    }

    private static final Comparator<Object> INTERCEPTOR_ORDER_COMPARATOR = OrderComparator.INSTANCE.withSourceProvider(object -> {
        if (object instanceof InterceptorRegistration) {
            return (Ordered) ((InterceptorRegistration) object)::getOrder;
        }
        return null;
    });

}
