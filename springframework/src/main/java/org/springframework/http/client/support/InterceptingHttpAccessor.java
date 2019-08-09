package org.springframework.http.client.support;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class InterceptingHttpAccessor extends HttpAccessor {

    private final List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();

    @Nullable
    private volatile ClientHttpRequestFactory interceptingRequestFactory;

    public void setInterceptors(List<ClientHttpRequestInterceptor> interceptors) {
        // Take getInterceptors() List as-is when passed in here
        if (this.interceptors != interceptors) {
            this.interceptors.clear();
            this.interceptors.addAll(interceptors);
            AnnotationAwareOrderComparator.sort(this.interceptors);
        }
    }

    public List<ClientHttpRequestInterceptor> getInterceptors() {
        return this.interceptors;
    }

    @Override
    public void setRequestFactory(ClientHttpRequestFactory requestFactory) {
        super.setRequestFactory(requestFactory);
        this.interceptingRequestFactory = null;
    }

    @Override
    public ClientHttpRequestFactory getRequestFactory() {
        List<ClientHttpRequestInterceptor> interceptors = getInterceptors();
        if (!CollectionUtils.isEmpty(interceptors)) {
            ClientHttpRequestFactory factory = this.interceptingRequestFactory;
            if (factory == null) {
                factory = new InterceptingClientHttpRequestFactory(super.getRequestFactory(), interceptors);
                this.interceptingRequestFactory = factory;
            }
            return factory;
        } else {
            return super.getRequestFactory();
        }
    }

}
