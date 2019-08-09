package org.springframework.web.context.request;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.lang.Nullable;

public class SessionScope extends AbstractRequestAttributesScope {

    @Override
    protected int getScope() {
        return RequestAttributes.SCOPE_SESSION;
    }

    @Override
    public String getConversationId() {
        return RequestContextHolder.currentRequestAttributes().getSessionId();
    }

    @Override
    public Object get(String name, ObjectFactory<?> objectFactory) {
        Object mutex = RequestContextHolder.currentRequestAttributes().getSessionMutex();
        synchronized (mutex) {
            return super.get(name, objectFactory);
        }
    }

    @Override
    @Nullable
    public Object remove(String name) {
        Object mutex = RequestContextHolder.currentRequestAttributes().getSessionMutex();
        synchronized (mutex) {
            return super.remove(name);
        }
    }

}
