package org.springframework.context.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

public class SourceFilteringListener implements GenericApplicationListener, SmartApplicationListener {

    private final Object source;

    @Nullable
    private GenericApplicationListener delegate;

    public SourceFilteringListener(Object source, ApplicationListener<?> delegate) {
        this.source = source;
        this.delegate = (delegate instanceof GenericApplicationListener ? (GenericApplicationListener) delegate : new GenericApplicationListenerAdapter(delegate));
    }

    protected SourceFilteringListener(Object source) {
        this.source = source;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event.getSource() == this.source) {
            onApplicationEventInternal(event);
        }
    }

    @Override
    public boolean supportsEventType(ResolvableType eventType) {
        return (this.delegate == null || this.delegate.supportsEventType(eventType));
    }

    @Override
    public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        return supportsEventType(ResolvableType.forType(eventType));
    }

    @Override
    public boolean supportsSourceType(@Nullable Class<?> sourceType) {
        return (sourceType != null && sourceType.isInstance(this.source));
    }

    @Override
    public int getOrder() {
        return (this.delegate != null ? this.delegate.getOrder() : Ordered.LOWEST_PRECEDENCE);
    }

    protected void onApplicationEventInternal(ApplicationEvent event) {
        if (this.delegate == null) {
            throw new IllegalStateException("Must specify a delegate object or override the onApplicationEventInternal method");
        }
        this.delegate.onApplicationEvent(event);
    }

}
