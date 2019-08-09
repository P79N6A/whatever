package org.springframework.context.event;

import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;

import java.util.Map;

public class GenericApplicationListenerAdapter implements GenericApplicationListener, SmartApplicationListener {

    private static final Map<Class<?>, ResolvableType> eventTypeCache = new ConcurrentReferenceHashMap<>();

    private final ApplicationListener<ApplicationEvent> delegate;

    @Nullable
    private final ResolvableType declaredEventType;

    @SuppressWarnings("unchecked")
    public GenericApplicationListenerAdapter(ApplicationListener<?> delegate) {
        Assert.notNull(delegate, "Delegate listener must not be null");
        this.delegate = (ApplicationListener<ApplicationEvent>) delegate;
        this.declaredEventType = resolveDeclaredEventType(this.delegate);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        this.delegate.onApplicationEvent(event);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean supportsEventType(ResolvableType eventType) {
        if (this.delegate instanceof SmartApplicationListener) {
            Class<? extends ApplicationEvent> eventClass = (Class<? extends ApplicationEvent>) eventType.resolve();
            return (eventClass != null && ((SmartApplicationListener) this.delegate).supportsEventType(eventClass));
        } else {
            return (this.declaredEventType == null || this.declaredEventType.isAssignableFrom(eventType));
        }
    }

    @Override
    public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        return supportsEventType(ResolvableType.forClass(eventType));
    }

    @Override
    public boolean supportsSourceType(@Nullable Class<?> sourceType) {
        return !(this.delegate instanceof SmartApplicationListener) || ((SmartApplicationListener) this.delegate).supportsSourceType(sourceType);
    }

    @Override
    public int getOrder() {
        return (this.delegate instanceof Ordered ? ((Ordered) this.delegate).getOrder() : Ordered.LOWEST_PRECEDENCE);
    }

    @Nullable
    private static ResolvableType resolveDeclaredEventType(ApplicationListener<ApplicationEvent> listener) {
        ResolvableType declaredEventType = resolveDeclaredEventType(listener.getClass());
        if (declaredEventType == null || declaredEventType.isAssignableFrom(ApplicationEvent.class)) {
            Class<?> targetClass = AopUtils.getTargetClass(listener);
            if (targetClass != listener.getClass()) {
                declaredEventType = resolveDeclaredEventType(targetClass);
            }
        }
        return declaredEventType;
    }

    @Nullable
    static ResolvableType resolveDeclaredEventType(Class<?> listenerType) {
        ResolvableType eventType = eventTypeCache.get(listenerType);
        if (eventType == null) {
            eventType = ResolvableType.forClass(listenerType).as(ApplicationListener.class).getGeneric();
            eventTypeCache.put(listenerType, eventType);
        }
        return (eventType != ResolvableType.NONE ? eventType : null);
    }

}
